package com.ecommerce.marketplace.delivery.service;

import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.delivery.dto.ShipmentItemResponse;
import com.ecommerce.marketplace.delivery.dto.ShipmentResponse;
import com.ecommerce.marketplace.delivery.model.Shipment;
import com.ecommerce.marketplace.delivery.model.ShipmentItem;
import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import com.ecommerce.marketplace.delivery.repository.ShipmentItemRepository;
import com.ecommerce.marketplace.delivery.repository.ShipmentRepository;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final OrderRepository orderRepository;
    private final com.ecommerce.marketplace.notification.service.NotificationHub notificationHub;

    @Transactional
    public List<ShipmentResponse> createShipmentsForOrder(Order order) {
        List<Shipment> existing = shipmentRepository.findByOrderId(order.getId());
        if (!existing.isEmpty()) {
            return existing.stream().map(this::toResponse).collect(Collectors.toList());
        }

        // Group order items by sellerId (and warehouseId)
        Map<UUID, List<OrderItem>> itemsBySeller = order.getItems().stream()
            .collect(Collectors.groupingBy(OrderItem::getSellerId));

        List<Shipment> createdShipments = new ArrayList<>();

        for (Map.Entry<UUID, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            UUID sellerId = entry.getKey();
            List<OrderItem> items = entry.getValue();

            UUID warehouseId = items.get(0).getFulfillmentWarehouseId();
            String awb = generateAwbNumber();

            Shipment shipment = Shipment.builder()
                .orderId(order.getId())
                .sellerId(sellerId)
                .warehouseId(warehouseId)
                .awbNumber(awb)
                .status(ShipmentStatus.CREATED)
                .carrierName("INTERNAL_FLEET")
                .shippingAddressSnapshot(order.getShippingAddressSnapshot())
                .isCod(false)
                .codAmountPaisa(0L)
                .build();

            shipment = shipmentRepository.save(shipment);

            for (OrderItem orderItem : items) {
                ShipmentItem shipmentItem = ShipmentItem.builder()
                    .shipment(shipment)
                    .orderItemId(orderItem.getId())
                    .variantId(orderItem.getVariantId())
                    .qty(orderItem.getQty())
                    .build();
                shipment.getItems().add(shipmentItem);
            }

            shipment = shipmentRepository.save(shipment);
            createdShipments.add(shipment);
        }

        log.info("Created {} shipments for order {}", createdShipments.size(), order.getOrderNumber());
        return createdShipments.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ShipmentResponse assignDeliveryPartner(UUID shipmentId, UUID deliveryPartnerId) {
        Shipment shipment = getShipmentOrThrow(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.CREATED && shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new BusinessRuleException(
                "Cannot assign partner to shipment in status " + shipment.getStatus(),
                "INVALID_SHIPMENT_STATUS"
            );
        }

        shipment.setDeliveryPartnerId(deliveryPartnerId);
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipment = shipmentRepository.save(shipment);

        log.info("Assigned shipment {} (AWB: {}) to partner {}", shipmentId, shipment.getAwbNumber(), deliveryPartnerId);
        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse pickupShipment(UUID partnerId, UUID shipmentId) {
        Shipment shipment = getShipmentOrThrow(shipmentId);
        validatePartnerOwnership(shipment, partnerId);

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.PICKED_UP)) {
            throw new BusinessRuleException(
                "Cannot transition from " + shipment.getStatus() + " to PICKED_UP",
                "INVALID_STATE_TRANSITION"
            );
        }

        shipment.setStatus(ShipmentStatus.PICKED_UP);
        shipment = shipmentRepository.save(shipment);
        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse markOutForDelivery(UUID partnerId, UUID shipmentId) {
        Shipment shipment = getShipmentOrThrow(shipmentId);
        validatePartnerOwnership(shipment, partnerId);

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)) {
            throw new BusinessRuleException(
                "Cannot transition from " + shipment.getStatus() + " to OUT_FOR_DELIVERY",
                "INVALID_STATE_TRANSITION"
            );
        }

        // Generate 4-digit OTP for secure doorstep handshake
        String otp = String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
        shipment.setDeliveryOtp(otp);
        shipment.setOtpAttempts(0);
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment = shipmentRepository.save(shipment);

        log.info("Shipment {} is OUT_FOR_DELIVERY. Generated delivery OTP: {}", shipment.getAwbNumber(), otp);

        final String awbNumber = shipment.getAwbNumber();
        final UUID parentOrderId = shipment.getOrderId();

        try {
            orderRepository.findById(parentOrderId).ifPresent(order ->
                notificationHub.dispatchOutForDelivery(order.getCustomerId(), null, awbNumber, otp)
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch delivery notification for shipment {}: {}", awbNumber, e.getMessage());
        }

        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse verifyDeliveryOtp(UUID partnerId, UUID shipmentId, String otp) {
        Shipment shipment = getShipmentOrThrow(shipmentId);
        validatePartnerOwnership(shipment, partnerId);

        if (shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new BusinessRuleException(
                "Shipment must be OUT_FOR_DELIVERY to verify OTP, current status: " + shipment.getStatus(),
                "INVALID_SHIPMENT_STATUS"
            );
        }

        if (shipment.getOtpAttempts() >= shipment.getMaxOtpAttempts()) {
            throw new BusinessRuleException(
                "Maximum delivery OTP attempts exceeded. Contact support.",
                "OTP_ATTEMPTS_EXCEEDED"
            );
        }

        if (shipment.getDeliveryOtp() == null || !shipment.getDeliveryOtp().equals(otp.trim())) {
            shipment.setOtpAttempts(shipment.getOtpAttempts() + 1);
            shipmentRepository.save(shipment);
            int remaining = shipment.getMaxOtpAttempts() - shipment.getOtpAttempts();
            throw new BusinessRuleException(
                "Invalid delivery OTP. Remaining attempts: " + remaining,
                "INVALID_DELIVERY_OTP"
            );
        }

        // OTP verified — complete delivery
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(Instant.now());
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment {} successfully DELIVERED via OTP handshake", shipment.getAwbNumber());

        // Check if all shipments for the parent order are DELIVERED
        checkAndCompleteOrder(shipment.getOrderId());

        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse failDelivery(UUID partnerId, UUID shipmentId, String reason) {
        Shipment shipment = getShipmentOrThrow(shipmentId);
        validatePartnerOwnership(shipment, partnerId);

        shipment.setStatus(ShipmentStatus.FAILED);
        shipment.setFailedReason(reason);
        shipment = shipmentRepository.save(shipment);
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(UUID shipmentId) {
        return toResponse(getShipmentOrThrow(shipmentId));
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ShipmentResponse> getPartnerShipments(UUID partnerId, ShipmentStatus status, Pageable pageable) {
        Page<Shipment> page = status != null
            ? shipmentRepository.findByDeliveryPartnerIdAndStatus(partnerId, status, pageable)
            : shipmentRepository.findByDeliveryPartnerId(partnerId, pageable);

        List<ShipmentResponse> data = page.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    private void checkAndCompleteOrder(UUID orderId) {
        List<Shipment> orderShipments = shipmentRepository.findByOrderId(orderId);
        boolean allDelivered = !orderShipments.isEmpty() &&
            orderShipments.stream().allMatch(s -> s.getStatus() == ShipmentStatus.DELIVERED);

        if (allDelivered) {
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PROCESSING) {
                    order.setStatus(OrderStatus.SHIPPED);
                }
                if (order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
                    order.setStatus(OrderStatus.DELIVERED);
                    orderRepository.save(order);
                    log.info("All shipments delivered. Order {} advanced to DELIVERED", order.getOrderNumber());
                }
            });
        }
    }

    private Shipment getShipmentOrThrow(UUID shipmentId) {
        return shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId.toString()));
    }

    private void validatePartnerOwnership(Shipment shipment, UUID partnerId) {
        if (shipment.getDeliveryPartnerId() == null || !shipment.getDeliveryPartnerId().equals(partnerId)) {
            throw new BusinessRuleException("Shipment is not assigned to this delivery partner", "FORBIDDEN");
        }
    }

    public ShipmentResponse toResponse(Shipment s) {
        List<ShipmentItemResponse> itemResponses = s.getItems() != null
            ? s.getItems().stream().map(i -> ShipmentItemResponse.builder()
                .id(i.getId())
                .orderItemId(i.getOrderItemId())
                .variantId(i.getVariantId())
                .qty(i.getQty())
                .build()).collect(Collectors.toList())
            : Collections.emptyList();

        return ShipmentResponse.builder()
            .id(s.getId())
            .orderId(s.getOrderId())
            .sellerId(s.getSellerId())
            .warehouseId(s.getWarehouseId())
            .awbNumber(s.getAwbNumber())
            .status(s.getStatus())
            .carrierName(s.getCarrierName())
            .trackingUrl(s.getTrackingUrl())
            .deliveryPartnerId(s.getDeliveryPartnerId())
            .deliveryOtp(s.getDeliveryOtp())
            .otpAttempts(s.getOtpAttempts())
            .estimatedDeliveryAt(s.getEstimatedDeliveryAt())
            .deliveredAt(s.getDeliveredAt())
            .failedReason(s.getFailedReason())
            .recipientName(s.getRecipientName())
            .recipientPhone(s.getRecipientPhone())
            .shippingAddressSnapshot(s.getShippingAddressSnapshot())
            .codAmountPaisa(s.getCodAmountPaisa())
            .isCod(s.getIsCod())
            .items(itemResponses)
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }

    private String generateAwbNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "AWB-" + dateStr + "-" + rand;
    }
}
