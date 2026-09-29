package com.ecommerce.marketplace.delivery.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.delivery.dto.AssignDeliveryPartnerRequest;
import com.ecommerce.marketplace.delivery.dto.DeliveryTrackingResponse;
import com.ecommerce.marketplace.delivery.dto.ShipmentResponse;
import com.ecommerce.marketplace.delivery.service.DeliveryTrackingService;
import com.ecommerce.marketplace.delivery.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Endpoints for managing and tracking order shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final DeliveryTrackingService trackingService;

    @GetMapping("/{id}")
    @Operation(summary = "Get shipment details by ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipment(@PathVariable UUID id) {
        ShipmentResponse response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all shipments for an order")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipmentsByOrderId(@PathVariable UUID orderId) {
        List<ShipmentResponse> response = shipmentService.getShipmentsByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Get live tracking info for a shipment (AWB, status, live GPS)")
    public ResponseEntity<ApiResponse<DeliveryTrackingResponse>> getTracking(@PathVariable UUID id) {
        DeliveryTrackingResponse response = trackingService.getTracking(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Assign a shipment to a delivery partner agent")
    public ResponseEntity<ApiResponse<ShipmentResponse>> assignPartner(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDeliveryPartnerRequest request) {
        ShipmentResponse response = shipmentService.assignDeliveryPartner(id, request.getDeliveryPartnerId());
        return ResponseEntity.ok(ApiResponse.success("Delivery partner assigned", response));
    }
}
