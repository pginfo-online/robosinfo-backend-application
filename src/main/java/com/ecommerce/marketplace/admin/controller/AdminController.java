package com.ecommerce.marketplace.admin.controller;



import com.ecommerce.marketplace.admin.dto.AdminMetricsOverviewResponse;
import com.ecommerce.marketplace.admin.dto.RejectReasonRequest;
import com.ecommerce.marketplace.admin.service.AdminService;
import com.ecommerce.marketplace.catalog.dto.ProductResponse;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.seller.dto.SellerProfileResponse;
import com.ecommerce.marketplace.seller.model.SellerStatus;
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
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Operations & Governance", description = "Endpoints for platform administration, seller approval, product moderation, and business health metrics")
public class AdminController {

    private final AdminService adminService;

    // ── Seller Approval / KYC ────────────────────────────────────
    @PostMapping("/sellers/{id}/approve")
    @Operation(summary = "Approve seller KYC application and activate seller account")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> approveSeller(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        SellerProfileResponse response = adminService.approveSeller(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", response));
    }

    @PostMapping("/sellers/{id}/reject")
    @Operation(summary = "Reject seller KYC application with reason")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> rejectSeller(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RejectReasonRequest request) {
        SellerProfileResponse response = adminService.rejectSeller(principal.getId(), id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Seller rejected", response));
    }

    @GetMapping("/sellers")
    @Operation(summary = "List sellers with optional status filter (REGISTERED, VERIFIED, REJECTED)")
    public ResponseEntity<ApiResponse<PageResponse<SellerProfileResponse>>> getSellers(
            @RequestParam(required = false) SellerStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SellerProfileResponse> response = adminService.getSellers(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sellers/{id}/documents/{docId}/verify")
    @Operation(summary = "Approve or reject a specific KYC document")
    public ResponseEntity<ApiResponse<Void>> verifyDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable UUID docId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {
        adminService.verifySellerDocument(principal.getId(), docId, approved, reason);
        return ResponseEntity.ok(ApiResponse.success("Document verification updated", null));
    }

    // ── Product Moderation ───────────────────────────────────────
    @PostMapping("/products/{id}/approve")
    @Operation(summary = "Approve product for marketplace catalog publishing")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ProductResponse response = adminService.approveProduct(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Product approved", response));
    }

    @PostMapping("/products/{id}/reject")
    @Operation(summary = "Reject product listing with reason")
    public ResponseEntity<ApiResponse<ProductResponse>> rejectProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RejectReasonRequest request) {
        ProductResponse response = adminService.rejectProduct(principal.getId(), id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Product rejected", response));
    }

    @GetMapping("/products/pending")
    @Operation(summary = "List products pending moderation review")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getPendingProducts(
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ProductResponse> response = adminService.getPendingProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Platform Health & Metrics ────────────────────────────────
    @GetMapping("/metrics/overview")
    @Operation(summary = "Get platform overview metrics: GMV, order volume, active sellers, catalog counts, and disputes")
    public ResponseEntity<ApiResponse<AdminMetricsOverviewResponse>> getOverviewMetrics() {
        AdminMetricsOverviewResponse response = adminService.getPlatformMetrics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
