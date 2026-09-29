package com.ecommerce.marketplace.inventory.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.inventory.dto.*;
import com.ecommerce.marketplace.inventory.service.InventoryService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints for stock tracking, adjustments, and reservations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SellerService sellerService;

    @PostMapping("/adjust")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Adjust physical inventory quantity (add or deduct stock)")
    public ResponseEntity<ApiResponse<InventoryStockResponse>> adjustStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdjustStockRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        InventoryStockResponse response = inventoryService.adjustStock(seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", response));
    }

    @GetMapping("/my-stock")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get paginated, enriched stock details for authenticated seller")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDetailResponse>>> getMyStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<InventoryDetailResponse> response = inventoryService.getMyStockEnriched(
            seller.getId(), pageable, status, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get inventory adjustment history and audit logs")
    public ResponseEntity<ApiResponse<PageResponse<StockAuditLogResponse>>> getInventoryLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<StockAuditLogResponse> response = inventoryService.getInventoryAuditLogs(seller.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{variantId}/safety-stock")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update safety buffer threshold for a variant")
    public ResponseEntity<ApiResponse<Void>> updateSafetyStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID variantId,
            @RequestBody Map<String, Integer> body) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        int safetyStock = body.getOrDefault("safetyStock", 10);
        inventoryService.updateSafetyStock(seller.getId(), variantId, safetyStock);
        return ResponseEntity.ok(ApiResponse.success("Safety stock threshold updated", null));
    }

    @GetMapping("/variant/{variantId}")
    @Operation(summary = "Get stock for variant by variant ID and seller ID")
    public ResponseEntity<ApiResponse<InventoryStockResponse>> getStock(
            @PathVariable UUID variantId,
            @RequestParam UUID sellerId) {
        InventoryStockResponse response = inventoryService.getStock(variantId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for checkout (concurrency safe)")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        ReservationResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved", response));
    }
}
