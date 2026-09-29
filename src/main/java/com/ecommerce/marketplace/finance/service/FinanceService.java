package com.ecommerce.marketplace.finance.service;

import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.finance.dto.FinanceOverviewResponse;
import com.ecommerce.marketplace.finance.dto.SettlementResponse;
import com.ecommerce.marketplace.finance.dto.TransactionItemResponse;
import com.ecommerce.marketplace.finance.model.*;
import com.ecommerce.marketplace.finance.repository.LedgerAccountRepository;
import com.ecommerce.marketplace.finance.repository.LedgerEntryRepository;
import com.ecommerce.marketplace.finance.repository.SellerSettlementRepository;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.repository.OrderItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;
    private final SellerSettlementRepository settlementRepository;
    private final OrderItemRepository orderItemRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void postDoubleEntry(UUID debitAccountId, UUID creditAccountId, long amountPaisa,
                                String referenceType, UUID referenceId, String description) {
        if (amountPaisa <= 0) {
            throw new BusinessRuleException("Transaction amount must be positive", "INVALID_AMOUNT");
        }

        LedgerAccount debitAcc = accountRepository.findById(debitAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Ledger Account", debitAccountId.toString()));
        LedgerAccount creditAcc = accountRepository.findById(creditAccountId)
            .orElseThrow(() -> new ResourceNotFoundException("Ledger Account", creditAccountId.toString()));

        UUID transactionId = UUID.randomUUID();

        // 1. Post Debit entry
        LedgerEntry debitEntry = LedgerEntry.builder()
            .transactionId(transactionId)
            .accountId(debitAccountId)
            .entryType(EntryType.DEBIT)
            .amountPaisa(amountPaisa)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .description(description)
            .build();
        entryRepository.save(debitEntry);

        // 2. Post Credit entry
        LedgerEntry creditEntry = LedgerEntry.builder()
            .transactionId(transactionId)
            .accountId(creditAccountId)
            .entryType(EntryType.CREDIT)
            .amountPaisa(amountPaisa)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .description(description)
            .build();
        entryRepository.save(creditEntry);

        // 3. Update account balances according to normal debit/credit rules
        updateAccountBalance(debitAcc, EntryType.DEBIT, amountPaisa);
        updateAccountBalance(creditAcc, EntryType.CREDIT, amountPaisa);
        accountRepository.save(debitAcc);
        accountRepository.save(creditAcc);

        log.info("Posted double-entry transaction {}: Debit {} ({}), Credit {} ({}) for amount {} paisa",
            transactionId, debitAcc.getName(), debitAcc.getAccountType(),
            creditAcc.getName(), creditAcc.getAccountType(), amountPaisa);
    }

    private void updateAccountBalance(LedgerAccount acc, EntryType entryType, long amount) {
        boolean isDebitNormal = (acc.getAccountType() == AccountType.ASSET || acc.getAccountType() == AccountType.EXPENSE);

        if (entryType == EntryType.DEBIT) {
            acc.setBalancePaisa(isDebitNormal ? acc.getBalancePaisa() + amount : acc.getBalancePaisa() - amount);
        } else {
            acc.setBalancePaisa(isDebitNormal ? acc.getBalancePaisa() - amount : acc.getBalancePaisa() + amount);
        }
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getSellerSettlements(UUID sellerId) {
        return settlementRepository.findBySellerIdOrderByPeriodEndDesc(sellerId).stream()
            .map(this::toSettlementResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> getSellerSettlementsPaginated(UUID sellerId, Pageable pageable) {
        Page<SellerSettlement> page = settlementRepository.findBySellerIdOrderByPeriodEndDesc(sellerId, pageable);
        List<SettlementResponse> list = page.getContent().stream()
            .map(this::toSettlementResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(list, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public FinanceOverviewResponse getFinanceOverview(UUID sellerId) {
        List<SellerSettlement> settlements = settlementRepository.findBySellerIdOrderByPeriodEndDesc(sellerId);

        long grossSalesPaisa = 0;
        long totalCommissionPaisa = 0;
        long totalTaxPaisa = 0;
        long settledThisMonthPaisa = 0;
        long upcomingPayoutPaisa = 0;
        int pendingCount = 0;

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);

        for (SellerSettlement s : settlements) {
            grossSalesPaisa += s.getGrossAmountPaisa() != null ? s.getGrossAmountPaisa() : 0;
            totalCommissionPaisa += s.getCommissionPaisa() != null ? s.getCommissionPaisa() : 0;
            totalTaxPaisa += s.getTaxOnCommissionPaisa() != null ? s.getTaxOnCommissionPaisa() : 0;

            if (s.getStatus() == SettlementStatus.PAID && s.getPaidAt() != null) {
                LocalDate paidDate = s.getPaidAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (!paidDate.isBefore(startOfMonth)) {
                    settledThisMonthPaisa += s.getNetAmountPaisa() != null ? s.getNetAmountPaisa() : 0;
                }
            } else if (s.getStatus() == SettlementStatus.CALCULATED || s.getStatus() == SettlementStatus.PENDING) {
                upcomingPayoutPaisa += s.getNetAmountPaisa() != null ? s.getNetAmountPaisa() : 0;
                pendingCount++;
            }
        }

        LocalDate nextPayoutDate = now.with(TemporalAdjusters.next(DayOfWeek.FRIDAY));

        return FinanceOverviewResponse.builder()
            .grossSalesPaisa(grossSalesPaisa)
            .totalCommissionPaisa(totalCommissionPaisa)
            .totalTaxPaisa(totalTaxPaisa)
            .settledThisMonthPaisa(settledThisMonthPaisa)
            .upcomingPayoutPaisa(upcomingPayoutPaisa)
            .upcomingPayoutDate(nextPayoutDate)
            .pendingSettlementsCount(pendingCount)
            .totalSettlementsCount(settlements.size())
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionItemResponse> getSellerTransactions(UUID sellerId, Pageable pageable) {
        Page<OrderItem> orderItems = orderItemRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);

        List<TransactionItemResponse> items = orderItems.getContent().stream()
            .map(item -> {
                long gross = item.getTotalPaisa() != null ? item.getTotalPaisa() : 0;
                int referralPct = 10; // 10% standard commission
                long commission = Math.round(gross * 0.10);
                long fixedFee = 1000L; // Rs 10 fixed closing fee
                long gstOnFee = Math.round((commission + fixedFee) * 0.18); // 18% GST on marketplace fees
                long tds = Math.round(gross * 0.01); // 1% TDS under Sec 194-O
                long netCredit = gross - (commission + fixedFee + gstOnFee + tds);

                String productTitle = "Product";
                try {
                    var node = objectMapper.readTree(item.getProductSnapshot());
                    productTitle = node.path("title").asText("Product");
                } catch (Exception ignored) {}

                String orderNumber = item.getOrder() != null ? item.getOrder().getOrderNumber() : "ORD-UNKNOWN";

                return TransactionItemResponse.builder()
                    .id(item.getId())
                    .orderNumber(orderNumber)
                    .productTitle(productTitle)
                    .grossAmountPaisa(gross)
                    .referralFeePercent(referralPct)
                    .commissionPaisa(commission)
                    .fixedFeePaisa(fixedFee)
                    .gstOnFeePaisa(gstOnFee)
                    .tdsPaisa(tds)
                    .netCreditPaisa(Math.max(0, netCredit))
                    .status(item.getStatus().name())
                    .createdAt(item.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return new PageResponse<>(items, orderItems.getTotalElements(), null, orderItems.hasNext());
    }

    @Transactional
    public SettlementResponse generateSettlement(UUID sellerId, LocalDate start, LocalDate end,
                                                 long grossPaisa, long commissionPaisa, long taxPaisa) {
        long netPayable = grossPaisa - commissionPaisa - taxPaisa;

        SellerSettlement settlement = SellerSettlement.builder()
            .sellerId(sellerId)
            .periodStart(start)
            .periodEnd(end)
            .grossAmountPaisa(grossPaisa)
            .commissionPaisa(commissionPaisa)
            .taxOnCommissionPaisa(taxPaisa)
            .netAmountPaisa(netPayable)
            .status(SettlementStatus.CALCULATED)
            .build();

        settlement = settlementRepository.save(settlement);
        return toSettlementResponse(settlement);
    }

    private SettlementResponse toSettlementResponse(SellerSettlement s) {
        return SettlementResponse.builder()
            .id(s.getId())
            .sellerId(s.getSellerId())
            .periodStart(s.getPeriodStart())
            .periodEnd(s.getPeriodEnd())
            .grossSalesPaisa(s.getGrossAmountPaisa())
            .commissionPaisa(s.getCommissionPaisa())
            .taxDeductedPaisa(s.getTaxOnCommissionPaisa())
            .netPayablePaisa(s.getNetAmountPaisa())
            .status(s.getStatus())
            .payoutReference(s.getPayoutReference())
            .paidAt(s.getPaidAt())
            .build();
    }
}
