package com.ecommerce.marketplace.order.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.order.dto.*;
import com.ecommerce.marketplace.order.model.OrderItemStatus;
import com.ecommerce.marketplace.order.service.OrderService;
import com.ecommerce.marketplace.seller.dto.SellerProfileResponse;
import com.ecommerce.marketplace.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders Management", description = "Endpoints for placing, tracking, and managing customer and seller orders")
public class OrderController {

    private final OrderService orderService;
    private final SellerService sellerService;

    // ─── Customer Endpoints ──────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create an order from active shopping cart (idempotent checkout)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrderFromCart(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get paginated order history for current authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<OrderResponse> response = orderService.getCustomerOrders(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details by order ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order details by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order before shipment and release reserved inventory")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Customer requested cancellation") String reason) {
        OrderResponse response = orderService.cancelOrder(principal.getId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Update order status (State machine transition)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", response));
    }

    // ─── Seller Order Endpoints ──────────────────────────────────────────

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get paginated seller orders filtered by item status and search")
    public ResponseEntity<ApiResponse<PageResponse<SellerOrderResponse>>> getSellerOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OrderItemStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<SellerOrderResponse> response = orderService.getSellerOrders(
            seller.getId(), status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/seller/{orderItemId}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller order item details by ID")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> getSellerOrderDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderItemId) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerOrderResponse response = orderService.getSellerOrderDetail(seller.getId(), orderItemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/seller/{orderItemId}/pack")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Mark seller order item as packed (ready for pickup/dispatch)")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> packOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderItemId) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerOrderResponse response = orderService.packOrderItem(seller.getId(), orderItemId);
        return ResponseEntity.ok(ApiResponse.success("Order packed successfully", response));
    }

    @PostMapping("/seller/{orderItemId}/ship")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Mark seller order item as shipped with carrier and tracking info")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> shipOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderItemId,
            @Valid @RequestBody ShipOrderRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerOrderResponse response = orderService.shipOrderItem(seller.getId(), orderItemId, request);
        return ResponseEntity.ok(ApiResponse.success("Order marked as shipped", response));
    }

    @PostMapping("/seller/{orderItemId}/cancel")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Cancel seller order item and release allocated stock")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> cancelSellerOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID orderItemId,
            @RequestBody(required = false) Map<String, String> body) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        String reason = body != null ? body.getOrDefault("reason", "Seller cancelled order") : "Seller cancelled order";
        SellerOrderResponse response = orderService.cancelOrderItem(seller.getId(), orderItemId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled by seller", response));
    }
}
