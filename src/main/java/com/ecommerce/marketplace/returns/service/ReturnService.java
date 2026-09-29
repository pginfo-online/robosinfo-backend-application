package com.ecommerce.marketplace.returns.service;

import com.ecommerce.marketplace.catalog.model.Category;
import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.repository.CategoryRepository;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.dto.AdjustStockRequest;
import com.ecommerce.marketplace.inventory.model.InventoryEventType;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.payment.dto.RefundRequest;
import com.ecommerce.marketplace.payment.model.PaymentIntent;
import com.ecommerce.marketplace.payment.model.PaymentStatus;
import com.ecommerce.marketplace.payment.repository.PaymentIntentRepository;
import com.ecommerce.marketplace.payment.service.PaymentService;
import com.ecommerce.marketplace.returns.dto.*;
import com.ecommerce.marketplace.returns.model.*;
import com.ecommerce.marketplace.returns.repository.ReturnInspectionRepository;
import com.ecommerce.marketplace.returns.repository.ReturnRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnInspectionRepository inspectionRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReturnResponse createReturnRequest(UUID customerId, CreateReturnRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId().toString()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessRuleException("Order does not belong to this customer", "FORBIDDEN");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Returns can only be requested for DELIVERED orders", "ORDER_NOT_DELIVERED");
        }

        OrderItem orderItem = order.getItems().stream()
            .filter(i -> i.getId().equals(request.getOrderItemId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Order Item", request.getOrderItemId().toString()));

        // Check if active return already exists for this order item
        var existing = returnRequestRepository.findByOrderItemIdAndStatusNot(orderItem.getId(), ReturnStatus.CANCELLED);
        if (existing.isPresent()) {
            throw new BusinessRuleException("An active return request already exists for this item", "RETURN_ALREADY_EXISTS");
        }

        // Return window validation
        int returnWindowDays = 7;
        try {
            // Find category return window
            UUID categoryId = orderItem.getVariantId(); // fallback
            var products = productRepository.findAll();
            for (Product p : products) {
                boolean hasVariant = p.getVariants().stream().anyMatch(v -> v.getId().equals(orderItem.getVariantId()));
                if (hasVariant) {
                    Category cat = categoryRepository.findById(p.getCategoryId()).orElse(null);
                    if (cat != null && cat.getReturnWindowDays() != null) {
                        returnWindowDays = cat.getReturnWindowDays();
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Check delivery date
        Instant orderUpdatedAt = order.getUpdatedAt() != null ? order.getUpdatedAt() : (order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now());
        if (Duration.between(orderUpdatedAt, Instant.now()).toDays() > returnWindowDays) {
            throw new BusinessRuleException(
                "Return window of " + returnWindowDays + " days has expired for this item",
                "RETURN_WINDOW_EXPIRED"
            );
        }

        String photosJson = null;
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            try {
                photosJson = objectMapper.writeValueAsString(request.getPhotoUrls());
            } catch (Exception e) {
                photosJson = "[]";
            }
        }

        String returnNumber = generateReturnNumber();

        ReturnRequest returnRequest = ReturnRequest.builder()
            .returnNumber(returnNumber)
            .customerId(customerId)
            .orderId(order.getId())
            .orderItemId(orderItem.getId())
            .variantId(orderItem.getVariantId())
            .sellerId(orderItem.getSellerId())
            .returnType(request.getReturnType() != null ? request.getReturnType() : ReturnType.REFUND)
            .reason(request.getReason())
            .customerNotes(request.getCustomerNotes())
            .photoUrls(photosJson)
            .status(ReturnStatus.REQUESTED)
            .pickupAddressSnapshot(order.getShippingAddressSnapshot())
            .build();

        returnRequest = returnRequestRepository.save(returnRequest);
        log.info("Created return request {} for order item {}", returnNumber, orderItem.getId());
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse approveReturn(UUID approverId, UUID returnId) {
        ReturnRequest ret = getReturnOrThrow(returnId);
        ret.setStatus(ReturnStatus.APPROVED);
        ret.setApprovedBy(approverId);
        ret = returnRequestRepository.save(ret);
        log.info("Approved return request {}", ret.getReturnNumber());
        return toResponse(ret);
    }

    @Transactional
    public ReturnResponse rejectReturn(UUID approverId, UUID returnId, String reason) {
        ReturnRequest ret = getReturnOrThrow(returnId);
        ret.setStatus(ReturnStatus.REJECTED);
        ret.setRejectionReason(reason);
        ret.setApprovedBy(approverId);
        ret = returnRequestRepository.save(ret);
        log.info("Rejected return request {}: {}", ret.getReturnNumber(), reason);
        return toResponse(ret);
    }

    @Transactional
    public ReturnResponse receiveReturnAtWarehouse(UUID returnId) {
        ReturnRequest ret = getReturnOrThrow(returnId);
        ret.setStatus(ReturnStatus.RECEIVED_AT_WAREHOUSE);
        ret = returnRequestRepository.save(ret);
        return toResponse(ret);
    }

    @Transactional
    public ReturnResponse inspectReturn(UUID inspectorId, UUID returnId, InspectReturnRequest request) {
        ReturnRequest ret = getReturnOrThrow(returnId);

        ReturnInspection inspection = ReturnInspection.builder()
            .returnRequest(ret)
            .warehouseId(request.getWarehouseId())
            .inspectedBy(inspectorId)
            .qcPassed(request.getQcPassed())
            .disposition(request.getDisposition())
            .inspectorNotes(request.getInspectorNotes())
            .defectDescription(request.getDefectDescription())
            .build();

        inspection = inspectionRepository.save(inspection);
        ret.setInspection(inspection);

        if (Boolean.TRUE.equals(request.getQcPassed())) {
            ret.setStatus(ReturnStatus.QC_PASSED);

            // If disposition is RESTOCK_AS_NEW, increment warehouse inventory
            if (request.getDisposition() == ReturnDisposition.RESTOCK_AS_NEW) {
                AdjustStockRequest adjustReq = AdjustStockRequest.builder()
                    .variantId(ret.getVariantId())
                    .warehouseId(request.getWarehouseId())
                    .qtyChange(1)
                    .eventType(InventoryEventType.STOCK_IN)
                    .reason("Return restock " + ret.getReturnNumber())
                    .build();
                inventoryService.adjustStock(ret.getSellerId(), adjustReq);
            }

            // Automated Reverse Payout / Refund
            if (ret.getReturnType() == ReturnType.REFUND) {
                processAutomatedRefund(ret, inspectorId);
                ret.setStatus(ReturnStatus.REFUNDED);
            } else {
                ret.setStatus(ReturnStatus.REPLACED);
            }
        } else {
            ret.setStatus(ReturnStatus.QC_FAILED);
        }

        ret = returnRequestRepository.save(ret);
        log.info("Inspected return {}: QC passed = {}, final status = {}",
            ret.getReturnNumber(), request.getQcPassed(), ret.getStatus());
        return toResponse(ret);
    }

    private void processAutomatedRefund(ReturnRequest ret, UUID initiatedBy) {
        try {
            List<PaymentIntent> intents = paymentIntentRepository.findByOrderId(ret.getOrderId());
            PaymentIntent capturedIntent = intents.stream()
                .filter(i -> i.getStatus() == PaymentStatus.CAPTURED)
                .findFirst()
                .orElse(null);

            if (capturedIntent != null) {
                // Find order item price
                Order order = orderRepository.findById(ret.getOrderId()).orElseThrow();
                long refundAmount = order.getItems().stream()
                    .filter(i -> i.getId().equals(ret.getOrderItemId()))
                    .mapToLong(OrderItem::getTotalPaisa)
                    .findFirst()
                    .orElse(capturedIntent.getAmountPaisa());

                RefundRequest refundReq = RefundRequest.builder()
                    .paymentIntentId(capturedIntent.getId())
                    .orderId(ret.getOrderId())
                    .orderItemId(ret.getOrderItemId())
                    .amountPaisa(refundAmount)
                    .reason("Return QC approved: " + ret.getReason())
                    .idempotencyKey(UUID.randomUUID())
                    .build();

                paymentService.createRefund(initiatedBy, refundReq);
                log.info("Automated refund processed for return {}", ret.getReturnNumber());
            }
        } catch (Exception e) {
            log.error("Failed to process automated refund for return {}: {}", ret.getReturnNumber(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ReturnResponse getReturnById(UUID returnId) {
        return toResponse(getReturnOrThrow(returnId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getCustomerReturns(UUID customerId, Pageable pageable) {
        Page<ReturnRequest> page = returnRequestRepository.findByCustomerId(customerId, pageable);
        List<ReturnResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> getSellerReturns(UUID sellerId, Pageable pageable) {
        Page<ReturnRequest> page = returnRequestRepository.findBySellerId(sellerId, pageable);
        List<ReturnResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    private ReturnRequest getReturnOrThrow(UUID returnId) {
        return returnRequestRepository.findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException("Return Request", returnId.toString()));
    }

    public ReturnResponse toResponse(ReturnRequest r) {
        ReturnInspectionResponse inspResp = null;
        if (r.getInspection() != null) {
            ReturnInspection i = r.getInspection();
            inspResp = ReturnInspectionResponse.builder()
                .id(i.getId())
                .warehouseId(i.getWarehouseId())
                .inspectedBy(i.getInspectedBy())
                .qcPassed(i.getQcPassed())
                .disposition(i.getDisposition())
                .inspectorNotes(i.getInspectorNotes())
                .defectDescription(i.getDefectDescription())
                .inspectedAt(i.getInspectedAt())
                .build();
        }

        return ReturnResponse.builder()
            .id(r.getId())
            .returnNumber(r.getReturnNumber())
            .customerId(r.getCustomerId())
            .orderId(r.getOrderId())
            .orderItemId(r.getOrderItemId())
            .variantId(r.getVariantId())
            .sellerId(r.getSellerId())
            .returnType(r.getReturnType())
            .reason(r.getReason())
            .customerNotes(r.getCustomerNotes())
            .photoUrls(r.getPhotoUrls())
            .status(r.getStatus())
            .pickupAddressSnapshot(r.getPickupAddressSnapshot())
            .rejectionReason(r.getRejectionReason())
            .approvedBy(r.getApprovedBy())
            .inspection(inspResp)
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .build();
    }

    private String generateReturnNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "RET-" + dateStr + "-" + rand;
    }
}
