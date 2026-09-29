package com.ecommerce.marketplace.finance.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.finance.dto.FinanceOverviewResponse;
import com.ecommerce.marketplace.finance.dto.PostJournalEntryRequest;
import com.ecommerce.marketplace.finance.dto.SettlementResponse;
import com.ecommerce.marketplace.finance.dto.TransactionItemResponse;
import com.ecommerce.marketplace.finance.service.FinanceService;
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

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
@Tag(name = "Finance & Settlements", description = "Endpoints for financial ledger auditing and seller payout settlements")
public class FinanceController {

    private final FinanceService financeService;
    private final SellerService sellerService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get aggregate financial overview (upcoming payouts, gross sales, fees, TDS)")
    public ResponseEntity<ApiResponse<FinanceOverviewResponse>> getOverview(
            @AuthenticationPrincipal UserPrincipal principal) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        FinanceOverviewResponse response = financeService.getFinanceOverview(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/settlements/my-settlements")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get settlement statements for authenticated seller (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<SettlementResponse>>> getMySettlements(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "periodEnd", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<SettlementResponse> settlements = financeService.getSellerSettlementsPaginated(seller.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get itemized order fee deductions and net credits (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionItemResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SellerProfileResponse seller = sellerService.getSellerByUserId(principal.getId());
        PageResponse<TransactionItemResponse> transactions = financeService.getSellerTransactions(seller.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Post manual double-entry journal voucher into ledger (Admin only)")
    public ResponseEntity<ApiResponse<Void>> postJournalEntry(
            @Valid @RequestBody PostJournalEntryRequest request) {
        financeService.postDoubleEntry(
            request.getDebitAccountId(),
            request.getCreditAccountId(),
            request.getAmountPaisa(),
            request.getReferenceType(),
            request.getReferenceId(),
            request.getDescription()
        );
        return ResponseEntity.ok(ApiResponse.success("Journal entry posted successfully", null));
    }
}
