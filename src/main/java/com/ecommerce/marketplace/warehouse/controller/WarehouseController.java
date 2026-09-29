package com.ecommerce.marketplace.warehouse.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.warehouse.dto.*;
import com.ecommerce.marketplace.warehouse.model.GrnStatus;
import com.ecommerce.marketplace.warehouse.service.WarehouseService;
import com.ecommerce.marketplace.warehouse.service.WmsInboundService;
import com.ecommerce.marketplace.warehouse.service.WmsOutboundService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Tag(name = "Warehouse Operations (WMS)", description = "APIs for warehouse facility layout, inbound GRN quality checks, and outbound pick-pack-ship operations")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WmsInboundService inboundService;
    private final WmsOutboundService outboundService;

    // ── Facilities ───────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new warehouse facility")
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(request);
        return ResponseEntity.ok(ApiResponse.success("Warehouse created", response));
    }

    @GetMapping
    @Operation(summary = "Get all active warehouse facilities")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getWarehouses() {
        List<WarehouseResponse> response = warehouseService.getAllActiveWarehouses();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse facility details by ID")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouse(@PathVariable UUID id) {
        WarehouseResponse response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Inbound GRN ──────────────────────────────────────────────
    @PostMapping("/grn")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Inward inventory from seller consignments (Create GRN)")
    public ResponseEntity<ApiResponse<GrnResponse>> createGrn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateGrnRequest request) {
        GrnResponse response = inboundService.createGrn(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("GRN created successfully", response));
    }

    @GetMapping("/grn/{id}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get GRN details with quality inspection results")
    public ResponseEntity<ApiResponse<GrnResponse>> getGrn(@PathVariable UUID id) {
        GrnResponse response = inboundService.getGrnById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/grn/{id}/complete")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Complete GRN quality check and inward passed items into warehouse inventory ledger")
    public ResponseEntity<ApiResponse<GrnResponse>> completeGrn(@PathVariable UUID id) {
        GrnResponse response = inboundService.completeGrn(id);
        return ResponseEntity.ok(ApiResponse.success("GRN completed and stock inwarded", response));
    }

    @GetMapping("/{id}/grns")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List GRNs for a warehouse")
    public ResponseEntity<ApiResponse<PageResponse<GrnResponse>>> getWarehouseGrns(
            @PathVariable UUID id,
            @RequestParam(required = false) GrnStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<GrnResponse> response = inboundService.getWarehouseGrns(id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Outbound Pick-Pack-Ship ──────────────────────────────────
    @PostMapping("/picklists/generate")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Generate picklist for pending order shipments")
    public ResponseEntity<ApiResponse<PicklistResponse>> generatePicklist(@Valid @RequestBody GeneratePicklistRequest request) {
        PicklistResponse response = outboundService.generatePicklist(request);
        return ResponseEntity.ok(ApiResponse.success("Picklist generated", response));
    }

    @PostMapping("/picklists/scan")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Scan item barcode or SKU during picking to verify picklist item")
    public ResponseEntity<ApiResponse<PicklistItemResponse>> scanItem(@Valid @RequestBody ScanItemRequest request) {
        PicklistItemResponse response = outboundService.scanItem(request);
        return ResponseEntity.ok(ApiResponse.success("Item scanned", response));
    }

    @PostMapping("/packing")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Record box weight/dimensions and generate packing slip for shipment")
    public ResponseEntity<ApiResponse<PackingSlipResponse>> createPackingSlip(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePackingSlipRequest request) {
        PackingSlipResponse response = outboundService.createPackingSlip(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Packing slip created", response));
    }

    @PostMapping("/manifests")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create dispatch manifest grouping packaged shipments for carrier vehicle pickup")
    public ResponseEntity<ApiResponse<DispatchManifestResponse>> createManifest(@Valid @RequestBody CreateManifestRequest request) {
        DispatchManifestResponse response = outboundService.createManifest(request);
        return ResponseEntity.ok(ApiResponse.success("Dispatch manifest created", response));
    }

    @PostMapping("/manifests/{id}/dispatch")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Mark manifest DISPATCHED and transition associated shipments to IN_TRANSIT")
    public ResponseEntity<ApiResponse<DispatchManifestResponse>> dispatchManifest(@PathVariable UUID id) {
        DispatchManifestResponse response = outboundService.dispatchManifest(id);
        return ResponseEntity.ok(ApiResponse.success("Manifest dispatched", response));
    }
}
