package com.ecommerce.marketplace.seller.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.seller.dto.*;
import com.ecommerce.marketplace.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Onboarding & Management", description = "Endpoints for seller registration, KYC documents, bank details, and store settings")
public class SellerController {

    private final SellerService sellerService;

    // ─── Registration ────────────────────────────────────────────────────

    /**
     * One-shot endpoint: creates a User account and a Seller profile in a single transaction.
     * Returns access + refresh tokens so the client can log in immediately after registration.
     */
    @PostMapping("/register-business")
    @Operation(summary = "Register a new seller business (creates user + seller profile and returns tokens)")
    public ResponseEntity<ApiResponse<SellerAuthResponse>> registerBusiness(
            @Valid @RequestBody RegisterBusinessRequest request) {
        SellerAuthResponse response = sellerService.registerBusiness(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Business registered successfully", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register currently authenticated user as a marketplace seller")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> registerSeller(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterSellerRequest request) {
        SellerProfileResponse response = sellerService.registerSeller(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller registered successfully", response));
    }

    // ─── Profile ─────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller profile for currently authenticated user")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getSellerProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse response = sellerService.getSellerByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Store Settings ───────────────────────────────────────────────────

    @GetMapping("/me/store-settings")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get store configuration and settings for authenticated seller")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        StoreSettingsResponse response = sellerService.getStoreSettings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/store-settings")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update store configuration settings (support email, description, pickup address, return policy)")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> updateStoreSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody StoreSettingsRequest request) {
        StoreSettingsResponse response = sellerService.updateStoreSettings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Store settings updated", response));
    }

    // ─── KYC Documents ───────────────────────────────────────────────────

    @PostMapping("/me/documents")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Upload or update a KYC document for seller verification")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DocumentUploadRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        DocumentResponse response = sellerService.uploadDocument(seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/me/documents")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "List all uploaded KYC documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        List<DocumentResponse> response = sellerService.getDocuments(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Bank Accounts ────────────────────────────────────────────────────

    @PostMapping("/me/bank-accounts")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Add bank account for settlement payouts")
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BankAccountRequest request) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        BankAccountResponse response = sellerService.addBankAccount(seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Bank account added", response));
    }

    @GetMapping("/me/bank-accounts")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "List registered bank accounts")
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getBankAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        List<BankAccountResponse> response = sellerService.getBankAccounts(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Onboarding Submission ────────────────────────────────────────────

    @PostMapping("/me/submit")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Submit onboarding application for admin review")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> submitForReview(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        SellerProfileResponse response = sellerService.submitForReview(seller.getId());
        return ResponseEntity.ok(ApiResponse.success("Application submitted for review", response));
    }
}
