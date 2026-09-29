package com.ecommerce.marketplace.finance.service;

import com.ecommerce.marketplace.finance.model.AccountType;
import com.ecommerce.marketplace.finance.model.LedgerAccount;
import com.ecommerce.marketplace.finance.model.LedgerEntry;
import com.ecommerce.marketplace.finance.repository.LedgerAccountRepository;
import com.ecommerce.marketplace.finance.repository.LedgerEntryRepository;
import com.ecommerce.marketplace.finance.repository.SellerSettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private LedgerAccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository entryRepository;

    @Mock
    private SellerSettlementRepository settlementRepository;

    @InjectMocks
    private FinanceService financeService;

    private LedgerAccount debitAccount;
    private LedgerAccount creditAccount;
    private UUID debitAccountId;
    private UUID creditAccountId;

    @BeforeEach
    void setUp() {
        debitAccountId = UUID.randomUUID();
        creditAccountId = UUID.randomUUID();

        // 1010 - Gateway (Asset)
        debitAccount = LedgerAccount.builder()
            .id(debitAccountId)
            .name("Payment Gateway Clearing")
            .accountType(AccountType.ASSET)
            .balancePaisa(0L)
            .build();

        // 2010 - Escrow (Liability)
        creditAccount = LedgerAccount.builder()
            .id(creditAccountId)
            .name("Customer Escrow Account")
            .accountType(AccountType.LIABILITY)
            .balancePaisa(0L)
            .build();
    }

    @Test
    @DisplayName("Should post double entry transaction correctly updating balances")
    void testPostDoubleEntry() {
        when(accountRepository.findById(debitAccountId)).thenReturn(Optional.of(debitAccount));
        when(accountRepository.findById(creditAccountId)).thenReturn(Optional.of(creditAccount));

        financeService.postDoubleEntry(
            debitAccountId,
            creditAccountId,
            50000L, // Rs. 500
            "CUSTOMER_ORDER",
            UUID.randomUUID(),
            "Payment for order"
        );

        // Verify two entries saved (one debit, one credit)
        verify(entryRepository, times(2)).save(any(LedgerEntry.class));

        // Asset account debited -> balance increases
        assertEquals(50000L, debitAccount.getBalancePaisa());

        // Liability account credited -> balance increases
        assertEquals(50000L, creditAccount.getBalancePaisa());

        verify(accountRepository).save(debitAccount);
        verify(accountRepository).save(creditAccount);
    }
}
