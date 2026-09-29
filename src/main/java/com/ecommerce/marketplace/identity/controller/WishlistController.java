package com.ecommerce.marketplace.identity.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.identity.dto.WishlistItemResponse;
import com.ecommerce.marketplace.identity.dto.WishlistResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.identity.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Customer Wishlist", description = "Endpoints for saving and managing customer favorite products")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get full wishlist details for authenticated customer")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        WishlistResponse response = wishlistService.getWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Add a product to customer wishlist")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addToWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {
        WishlistItemResponse response = wishlistService.addToWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product added to wishlist", response));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a product from customer wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {
        wishlistService.removeFromWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @GetMapping("/ids")
    @Operation(summary = "Get list of all product IDs in authenticated customer wishlist")
    public ResponseEntity<ApiResponse<List<UUID>>> getWishlistIds(@AuthenticationPrincipal UserPrincipal principal) {
        List<UUID> ids = wishlistService.getWishlistProductIds(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(ids));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if a product is in customer wishlist")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId) {
        boolean inWishlist = wishlistService.isInWishlist(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("inWishlist", inWishlist)));
    }
}
