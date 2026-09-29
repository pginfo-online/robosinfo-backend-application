package com.ecommerce.marketplace.catalog.controller;

import com.ecommerce.marketplace.catalog.dto.SellerListingDetailResponse;
import com.ecommerce.marketplace.catalog.dto.SellerListingRequest;
import com.ecommerce.marketplace.catalog.dto.SellerListingResponse;
import com.ecommerce.marketplace.catalog.dto.UpdateListingRequest;
import com.ecommerce.marketplace.catalog.service.SellerListingService;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Seller Listings & Pricing", description = "Endpoints for managing seller price listings and offers on variants")
public class SellerListingController {

    private final SellerListingService listingService;
    private final SellerService sellerService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create or update price listing for a product variant")
    public ResponseEntity<ApiResponse<SellerListingResponse>> createListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerListingRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerListingResponse response = listingService.createOrUpdateListing(seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Listing saved", response));
    }

    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get paginated, enriched listings for the authenticated seller")
    public ResponseEntity<ApiResponse<PageResponse<SellerListingDetailResponse>>> getMyListings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<SellerListingDetailResponse> listings = listingService.getMyListingsEnriched(
            seller.getId(), pageable, search, isActive);
        return ResponseEntity.ok(ApiResponse.success(listings));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get detailed seller listing by ID")
    public ResponseEntity<ApiResponse<SellerListingDetailResponse>> getListingById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerListingDetailResponse response = listingService.getListingDetail(seller.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update seller listing price, condition, or active state")
    public ResponseEntity<ApiResponse<SellerListingDetailResponse>> updateListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListingRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerListingDetailResponse response = listingService.updateListing(seller.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Listing updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Deactivate/delete seller listing")
    public ResponseEntity<ApiResponse<Void>> deleteListing(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        listingService.deleteListing(seller.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Listing deleted successfully", null));
    }

    @GetMapping("/variant/{variantId}")
    @Operation(summary = "Get all seller offers for a variant (sorted by lowest price)")
    public ResponseEntity<ApiResponse<List<SellerListingResponse>>> getVariantListings(@PathVariable UUID variantId) {
        List<SellerListingResponse> listings = listingService.getVariantListings(variantId);
        return ResponseEntity.ok(ApiResponse.success(listings));
    }
}
