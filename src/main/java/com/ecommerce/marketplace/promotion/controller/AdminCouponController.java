package com.ecommerce.marketplace.promotion.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.promotion.dto.CouponResponse;
import com.ecommerce.marketplace.promotion.dto.CreateCouponRequest;
import com.ecommerce.marketplace.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin Promotions & Coupons", description = "Endpoints for creating and managing promotional discount coupons")
public class AdminCouponController {

    private final PromotionService promotionService;

    @PostMapping
    @Operation(summary = "Create a new promotional coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        CouponResponse response = promotionService.createCoupon(request);
        return ResponseEntity.ok(ApiResponse.success("Coupon created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all coupons with usage metrics")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getAllCoupons(@PageableDefault(size = 20) Pageable pageable) {
        PageResponse<CouponResponse> response = promotionService.getAllCoupons(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleStatus(@PathVariable UUID id, @RequestParam boolean active) {
        CouponResponse response = promotionService.toggleCouponStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Coupon status updated", response));
    }
}
