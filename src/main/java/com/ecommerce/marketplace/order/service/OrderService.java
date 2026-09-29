package com.ecommerce.marketplace.order.service;

import com.ecommerce.marketplace.cart.dto.CartItemResponse;
import com.ecommerce.marketplace.cart.dto.CartResponse;
import com.ecommerce.marketplace.cart.service.CartService;
import com.ecommerce.marketplace.catalog.model.SellerListing;
import com.ecommerce.marketplace.catalog.repository.SellerListingRepository;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.identity.model.UserAddress;
import com.ecommerce.marketplace.identity.repository.UserAddressRepository;
import com.ecommerce.marketplace.inventory.dto.ReservationResponse;
import com.ecommerce.marketplace.inventory.dto.ReserveStockRequest;
import com.ecommerce.marketplace.inventory.model.InventoryReservation;
import com.ecommerce.marketplace.inventory.repository.InventoryReservationRepository;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import com.ecommerce.marketplace.order.dto.*;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderItemStatus;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderItemRepository;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final InventoryReservationRepository reservationRepository;
    private final UserAddressRepository addressRepository;
    private final SellerListingRepository listingRepository;
    private final ObjectMapper objectMapper;
    private final com.ecommerce.marketplace.promotion.service.PromotionService promotionService;
    private final com.ecommerce.marketplace.notification.service.NotificationHub notificationHub;

    @Transactional
    public OrderResponse createOrderFromCart(UUID customerId, CreateOrderRequest request) {
        // Idempotency check
        var existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOrder.isPresent()) {
            return toOrderResponse(existingOrder.get());
        }

        // Validate shipping address
        UserAddress address = addressRepository.findByIdAndUserId(request.getAddressId(), customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipping Address", request.getAddressId().toString()));

        String addressJson;
        try {
            addressJson = objectMapper.writeValueAsString(address);
        } catch (Exception e) {
            addressJson = "{\"city\":\"" + address.getCity() + "\",\"pincode\":\"" + address.getPincode() + "\"}";
        }

        // Fetch and validate active cart
        CartResponse cart = cartService.getCartDetails(customerId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cannot checkout with an empty cart", "CART_EMPTY");
        }

        // Verify stock and reserve for each item
        List<ReservationResponse> reservations = new ArrayList<>();
        for (CartItemResponse item : cart.getItems()) {
            if (!Boolean.TRUE.equals(item.getInStock())) {
                throw new BusinessRuleException("Item " + item.getProductTitle() + " is out of stock", "OUT_OF_STOCK");
            }

            SellerListing listing = listingRepository.findById(item.getSellerListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing", item.getSellerListingId().toString()));

            UUID reservationKey = UUID.randomUUID();
            ReserveStockRequest reserveReq = ReserveStockRequest.builder()
                .variantId(item.getVariantId())
                .sellerId(listing.getSellerId())
                .qty(item.getQty())
                .reservationKey(reservationKey)
                .build();

            ReservationResponse res = inventoryService.reserveStock(reserveReq);
            reservations.add(res);
        }

        // Calculate totals
        long subtotal = cart.getSubtotalPaisa();
        long discount = 0L;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discount = promotionService.validateAndCalculateDiscount(request.getCouponCode(), customerId, subtotal);
        }
        long taxableAmount = Math.max(0, subtotal - discount);
        long tax = (long) (taxableAmount * 0.18); // 18% GST standard
        long shipping = subtotal >= 50000L ? 0L : 4900L; // Free shipping above Rs. 500
        long total = taxableAmount + tax + shipping;

        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .customerId(customerId)
            .status(OrderStatus.CREATED)
            .shippingAddressSnapshot(addressJson)
            .subtotalPaisa(subtotal)
            .discountPaisa(discount)
            .taxPaisa(tax)
            .shippingPaisa(shipping)
            .totalPaisa(total)
            .couponCode(request.getCouponCode())
            .notes(request.getNotes())
            .idempotencyKey(request.getIdempotencyKey())
            .build();

        order = orderRepository.save(order);

        // Create OrderItems linked to reservations
        int idx = 0;
        for (CartItemResponse item : cart.getItems()) {
            SellerListing listing = listingRepository.findById(item.getSellerListingId()).orElseThrow();
            ReservationResponse res = reservations.get(idx++);

            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variantId(item.getVariantId())
                .sellerId(listing.getSellerId())
                .sellerListingId(item.getSellerListingId())
                .productSnapshot("{\"title\":\"" + item.getProductTitle() + "\",\"sku\":\"" + item.getSku() + "\"}")
                .qty(item.getQty())
                .unitPricePaisa(item.getUnitPricePaisa())
                .mrpPaisa(item.getMrpPaisa())
                .discountPaisa(Math.max(0, item.getMrpPaisa() - item.getUnitPricePaisa()) * item.getQty())
                .taxPaisa((long) (item.getSubtotalPaisa() * 0.18))
                .totalPaisa(item.getSubtotalPaisa() + (long)(item.getSubtotalPaisa() * 0.18))
                .status(OrderItemStatus.CREATED)
                .reservationId(res.getReservationId())
                .build();

            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        if (discount > 0 && request.getCouponCode() != null) {
            promotionService.recordCouponUsage(request.getCouponCode(), customerId, order.getId(), discount);
        }

        // Clear cart now that order is formed
        cartService.clearCart(customerId);

        try {
            notificationHub.dispatchOrderPlaced(
                customerId,
                null,
                order.getOrderNumber(),
                java.math.BigDecimal.valueOf(total).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch order notification for order {}: {}", order.getOrderNumber(), e.getMessage());
        }

        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderNumber));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getCustomerOrders(UUID customerId, Pageable pageable) {
        Page<Order> page = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderResponse> data = page.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getStatus().canTransitionTo(nextStatus)) {
            throw new BusinessRuleException(
                "Invalid state transition from " + order.getStatus() + " to " + nextStatus,
                "INVALID_STATE_TRANSITION"
            );
        }

        // Handle inventory confirmation or release
        if (nextStatus == OrderStatus.PAID) {
            confirmOrderInventory(order);
        } else if (nextStatus == OrderStatus.CANCELLED) {
            releaseOrderInventory(order);
        }

        order.setStatus(nextStatus);
        order = orderRepository.save(order);
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID customerId, UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    private void confirmOrderInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getReservationId() != null) {
                reservationRepository.findById(item.getReservationId()).ifPresent(res -> {
                    inventoryService.confirmReservation(res.getReservationKey());
                });
            }
        }
    }

    private void releaseOrderInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getReservationId() != null) {
                reservationRepository.findById(item.getReservationId()).ifPresent(res -> {
                    inventoryService.releaseReservation(res.getReservationKey());
                });
            }
        }
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(i -> OrderItemResponse.builder()
                .id(i.getId())
                .variantId(i.getVariantId())
                .sellerId(i.getSellerId())
                .sellerListingId(i.getSellerListingId())
                .productTitle(extractTitleFromSnapshot(i.getProductSnapshot()))
                .sku(extractSkuFromSnapshot(i.getProductSnapshot()))
                .qty(i.getQty())
                .unitPricePaisa(i.getUnitPricePaisa())
                .mrpPaisa(i.getMrpPaisa())
                .discountPaisa(i.getDiscountPaisa())
                .taxPaisa(i.getTaxPaisa())
                .totalPaisa(i.getTotalPaisa())
                .status(i.getStatus())
                .build())
            .collect(Collectors.toList());

        return OrderResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .status(order.getStatus())
            .shippingAddressSnapshot(order.getShippingAddressSnapshot())
            .subtotalPaisa(order.getSubtotalPaisa())
            .discountPaisa(order.getDiscountPaisa())
            .taxPaisa(order.getTaxPaisa())
            .shippingPaisa(order.getShippingPaisa())
            .totalPaisa(order.getTotalPaisa())
            .couponCode(order.getCouponCode())
            .notes(order.getNotes())
            .items(items)
            .createdAt(order.getCreatedAt())
            .build();
    }

    private String generateOrderNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "ORD-" + dateStr + "-" + rand;
    }

    private String extractTitleFromSnapshot(String snapshot) {
        try {
            var node = objectMapper.readTree(snapshot);
            return node.path("title").asText("Product");
        } catch (Exception e) {
            return "Product";
        }
    }

    private String extractSkuFromSnapshot(String snapshot) {
        try {
            var node = objectMapper.readTree(snapshot);
            return node.path("sku").asText("SKU");
        } catch (Exception e) {
            return "SKU";
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerOrderResponse> getSellerOrders(
            UUID sellerId, OrderItemStatus status, String search, Pageable pageable) {

        Page<OrderItem> page;
        if (status != null) {
            page = orderItemRepository.findBySellerIdAndStatus(sellerId, status, pageable);
        } else {
            page = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        }

        List<SellerOrderResponse> list = page.getContent().stream()
            .map(this::toSellerOrderResponse)
            .filter(o -> {
                if (search == null || search.isBlank()) return true;
                String q = search.toLowerCase().trim();
                return (o.getOrderNumber() != null && o.getOrderNumber().toLowerCase().contains(q))
                    || (o.getProductTitle() != null && o.getProductTitle().toLowerCase().contains(q))
                    || (o.getSku() != null && o.getSku().toLowerCase().contains(q))
                    || (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(q));
            })
            .collect(Collectors.toList());

        return new PageResponse<>(list, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public SellerOrderResponse getSellerOrderDetail(UUID sellerId, UUID orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new ResourceNotFoundException("OrderItem", orderItemId.toString()));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("Access denied to order item", "ACCESS_DENIED");
        }
        return toSellerOrderResponse(item);
    }

    @Transactional
    public SellerOrderResponse packOrderItem(UUID sellerId, UUID orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new ResourceNotFoundException("OrderItem", orderItemId.toString()));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("Access denied to order item", "ACCESS_DENIED");
        }
        item.setStatus(OrderItemStatus.PROCESSING);
        item = orderItemRepository.save(item);

        Order order = item.getOrder();
        if (order != null && order.getStatus() == OrderStatus.PAID) {
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
        }
        return toSellerOrderResponse(item);
    }

    @Transactional
    public SellerOrderResponse shipOrderItem(UUID sellerId, UUID orderItemId, ShipOrderRequest request) {
        OrderItem item = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new ResourceNotFoundException("OrderItem", orderItemId.toString()));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("Access denied to order item", "ACCESS_DENIED");
        }
        item.setStatus(OrderItemStatus.SHIPPED);
        item = orderItemRepository.save(item);

        Order order = item.getOrder();
        if (order != null) {
            boolean allShipped = order.getItems().stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.SHIPPED || i.getStatus() == OrderItemStatus.DELIVERED || i.getStatus() == OrderItemStatus.CANCELLED);
            if (allShipped && order.getStatus().canTransitionTo(OrderStatus.SHIPPED)) {
                order.setStatus(OrderStatus.SHIPPED);
                orderRepository.save(order);
            }
        }
        SellerOrderResponse resp = toSellerOrderResponse(item);
        resp.setCarrierName(request.getCarrierName());
        resp.setTrackingNumber(request.getTrackingNumber());
        return resp;
    }

    @Transactional
    public SellerOrderResponse cancelOrderItem(UUID sellerId, UUID orderItemId, String reason) {
        OrderItem item = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new ResourceNotFoundException("OrderItem", orderItemId.toString()));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("Access denied to order item", "ACCESS_DENIED");
        }
        item.setStatus(OrderItemStatus.CANCELLED);
        if (item.getReservationId() != null) {
            reservationRepository.findById(item.getReservationId()).ifPresent(res -> {
                inventoryService.releaseReservation(res.getReservationKey());
            });
        }
        item = orderItemRepository.save(item);
        return toSellerOrderResponse(item);
    }

    public SellerOrderResponse toSellerOrderResponse(OrderItem item) {
        Order order = item.getOrder();
        String customerName = "Customer";
        String customerPhone = "";
        String fullAddress = "";
        String city = "";
        String state = "";
        String pincode = "";

        if (order != null && order.getShippingAddressSnapshot() != null) {
            try {
                var node = objectMapper.readTree(order.getShippingAddressSnapshot());
                customerName = node.path("recipientName").asText(node.path("name").asText("Customer"));
                customerPhone = node.path("phone").asText("");
                String addr1 = node.path("addressLine1").asText(node.path("street").asText(""));
                String addr2 = node.path("addressLine2").asText("");
                city = node.path("city").asText("");
                state = node.path("state").asText("");
                pincode = node.path("pincode").asText(node.path("postalCode").asText(""));
                fullAddress = (addr1 + " " + addr2).trim();
            } catch (Exception ignored) {}
        }

        String productTitle = extractTitleFromSnapshot(item.getProductSnapshot());
        String sku = extractSkuFromSnapshot(item.getProductSnapshot());
        String imageUrl = null;
        try {
            var node = objectMapper.readTree(item.getProductSnapshot());
            imageUrl = node.path("imageUrl").asText(node.path("primaryImageUrl").asText(null));
        } catch (Exception ignored) {}

        return SellerOrderResponse.builder()
            .orderId(order != null ? order.getId() : null)
            .orderItemId(item.getId())
            .orderNumber(order != null ? order.getOrderNumber() : "N/A")
            .customerId(order != null ? order.getCustomerId() : null)
            .customerName(customerName)
            .customerPhone(customerPhone)
            .shippingAddress(fullAddress)
            .city(city)
            .state(state)
            .pincode(pincode)
            .variantId(item.getVariantId())
            .productTitle(productTitle)
            .sku(sku)
            .imageUrl(imageUrl)
            .qty(item.getQty())
            .unitPricePaisa(item.getUnitPricePaisa())
            .totalPaisa(item.getTotalPaisa())
            .taxPaisa(item.getTaxPaisa())
            .status(item.getStatus())
            .orderStatus(order != null ? order.getStatus().name() : "UNKNOWN")
            .createdAt(item.getCreatedAt() != null ? item.getCreatedAt() : (order != null ? order.getCreatedAt() : null))
            .build();
    }
}

