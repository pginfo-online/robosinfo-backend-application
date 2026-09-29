package com.ecommerce.marketplace.delivery.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.delivery.dto.ShipmentResponse;
import com.ecommerce.marketplace.delivery.dto.UpdateLocationRequest;
import com.ecommerce.marketplace.delivery.dto.VerifyDeliveryOtpRequest;
import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import com.ecommerce.marketplace.delivery.service.DeliveryTrackingService;
import com.ecommerce.marketplace.delivery.service.ShipmentService;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery Partner", description = "Operations for delivery partner agents, route fulfillment, and doorstep OTP verification")
public class DeliveryPartnerController {

    private final ShipmentService shipmentService;
    private final DeliveryTrackingService trackingService;

    @GetMapping("/shipments")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Get shipments assigned to current delivery partner")
    public ResponseEntity<ApiResponse<PageResponse<ShipmentResponse>>> getMyShipments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ShipmentStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ShipmentResponse> response = shipmentService.getPartnerShipments(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/shipments/{id}")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Get shipment details by ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/shipments/{id}/pickup")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Mark shipment as picked up from seller or warehouse")
    public ResponseEntity<ApiResponse<ShipmentResponse>> pickupShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ShipmentResponse response = shipmentService.pickupShipment(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Shipment picked up", response));
    }

    @PostMapping("/shipments/{id}/out-for-delivery")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Mark shipment OUT_FOR_DELIVERY and generate 4-digit customer delivery OTP")
    public ResponseEntity<ApiResponse<ShipmentResponse>> outForDelivery(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ShipmentResponse response = shipmentService.markOutForDelivery(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Shipment is out for delivery. OTP dispatched to customer.", response));
    }

    @PostMapping("/shipments/{id}/deliver")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Verify customer delivery OTP at doorstep to mark shipment DELIVERED")
    public ResponseEntity<ApiResponse<ShipmentResponse>> deliverShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody VerifyDeliveryOtpRequest request) {
        ShipmentResponse response = shipmentService.verifyDeliveryOtp(principal.getId(), id, request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("Delivery completed successfully", response));
    }

    @PostMapping("/shipments/{id}/fail")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Record delivery attempt failure (e.g. customer unavailable)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> failDelivery(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String reason) {
        ShipmentResponse response = shipmentService.failDelivery(principal.getId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Delivery marked as failed", response));
    }

    @PostMapping("/location")
    @PreAuthorize("hasRole('DELIVERY_PARTNER')")
    @Operation(summary = "Broadcast live GPS location for delivery partner")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateLocationRequest request) {
        trackingService.recordLocation(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Location updated", null));
    }
}
