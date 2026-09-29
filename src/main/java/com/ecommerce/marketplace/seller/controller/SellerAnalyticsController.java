package com.ecommerce.marketplace.seller.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.seller.dto.SellerDashboardMetricsResponse;
import com.ecommerce.marketplace.seller.dto.SellerProfileResponse;
import com.ecommerce.marketplace.seller.dto.SellerSalesAnalyticsResponse;
import com.ecommerce.marketplace.seller.service.SellerAnalyticsService;
import com.ecommerce.marketplace.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sellers/me/analytics")
@RequiredArgsConstructor
@Tag(name = "Seller Analytics & Performance", description = "Endpoints for seller dashboard KPI metrics and sales analytics charts")
public class SellerAnalyticsController {

    private final SellerAnalyticsService analyticsService;
    private final SellerService sellerService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get high-level KPI dashboard metrics (GMV today/month, pending orders, alerts, rating)")
    public ResponseEntity<ApiResponse<SellerDashboardMetricsResponse>> getDashboardMetrics(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerDashboardMetricsResponse metrics = analyticsService.getDashboardMetrics(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get sales analytics time series, top products, category split, and buyer ratings")
    public ResponseEntity<ApiResponse<SellerSalesAnalyticsResponse>> getSalesAnalytics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, defaultValue = "30d") String range) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerSalesAnalyticsResponse response = analyticsService.getSalesAnalytics(seller.getId(), range);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
