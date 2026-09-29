package com.ecommerce.marketplace.returns.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.returns.dto.CreateReturnRequest;
import com.ecommerce.marketplace.returns.dto.InspectReturnRequest;
import com.ecommerce.marketplace.returns.dto.ReturnResponse;
import com.ecommerce.marketplace.returns.service.ReturnService;
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
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Tag(name = "Returns & Reverse Logistics", description = "Endpoints for customer return requests, seller approval, warehouse inspection, and automated refund processing")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    @Operation(summary = "Initiate return request for a delivered order item within return window")
    public ResponseEntity<ApiResponse<ReturnResponse>> createReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReturnRequest request) {
        ReturnResponse response = returnService.createReturnRequest(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Return request submitted", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get paginated return requests for currently authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<ReturnResponse>>> getMyReturns(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReturnResponse> response = returnService.getCustomerReturns(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get return request details by ID")
    public ResponseEntity<ApiResponse<ReturnResponse>> getReturn(@PathVariable UUID id) {
        ReturnResponse response = returnService.getReturnById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Approve customer return request (Seller or Admin)")
    public ResponseEntity<ApiResponse<ReturnResponse>> approveReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        ReturnResponse response = returnService.approveReturn(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Return approved", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reject customer return request with reason")
    public ResponseEntity<ApiResponse<ReturnResponse>> rejectReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestParam String reason) {
        ReturnResponse response = returnService.rejectReturn(principal.getId(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Return rejected", response));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Record physical receipt of return consignment at warehouse facility")
    public ResponseEntity<ApiResponse<ReturnResponse>> receiveReturn(@PathVariable UUID id) {
        ReturnResponse response = returnService.receiveReturnAtWarehouse(id);
        return ResponseEntity.ok(ApiResponse.success("Return received at warehouse", response));
    }

    @PostMapping("/{id}/inspect")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Perform quality inspection & disposition; triggers automated refund if QC passes")
    public ResponseEntity<ApiResponse<ReturnResponse>> inspectReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody InspectReturnRequest request) {
        ReturnResponse response = returnService.inspectReturn(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Return inspection completed", response));
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get returns for current seller")
    public ResponseEntity<ApiResponse<PageResponse<ReturnResponse>>> getSellerReturns(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ReturnResponse> response = returnService.getSellerReturns(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
