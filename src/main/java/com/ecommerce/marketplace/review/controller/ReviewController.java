package com.ecommerce.marketplace.review.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.review.dto.*;
import com.ecommerce.marketplace.review.service.ReviewService;
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
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews & Ratings", description = "Endpoints for verified buyer product reviews, star rating calculations, and seller responses")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a verified buyer product review for a delivered order item")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", response));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get paginated approved reviews for a product")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReviewResponse> response = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/products/{productId}/summary")
    @Operation(summary = "Get aggregate rating breakdown and average star rating for a product")
    public ResponseEntity<ApiResponse<ProductRatingSummaryResponse>> getProductSummary(@PathVariable UUID productId) {
        ProductRatingSummaryResponse response = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/helpful")
    @Operation(summary = "Vote a review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> voteHelpful(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ReviewResponse response = reviewService.voteHelpful(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Voted helpful", response));
    }

    @PostMapping("/{id}/seller-response")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Post official seller response to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> addSellerResponse(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SellerReviewResponseRequest request) {
        ReviewResponse response = reviewService.addSellerResponse(principal.getId(), id, request.getResponse());
        return ResponseEntity.ok(ApiResponse.success("Seller response posted", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get reviews submitted by the authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReviewResponse> response = reviewService.getCustomerReviews(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
