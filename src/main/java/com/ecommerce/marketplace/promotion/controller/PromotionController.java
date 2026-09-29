package com.ecommerce.marketplace.promotion.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.promotion.dto.CouponValidationResponse;
import com.ecommerce.marketplace.promotion.dto.ValidateCouponRequest;
import com.ecommerce.marketplace.promotion.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecommerce.marketplace.promotion.dto.CouponResponse;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions/coupons")
@RequiredArgsConstructor
@Tag(name = "Promotions & Discounts", description = "Endpoints for customer coupon validation and discount calculations")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "List all currently active promotional coupons and offers for customers")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getActiveCoupons() {
        List<CouponResponse> coupons = promotionService.getActiveCoupons();
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate coupon code against active cart total and preview calculated discount")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        CouponValidationResponse response = promotionService.validateCouponPreview(request.getCode(), request.getCartTotalPaisa());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
