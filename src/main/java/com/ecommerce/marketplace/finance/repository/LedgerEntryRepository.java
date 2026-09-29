package com.ecommerce.marketplace.finance.repository;

import com.ecommerce.marketplace.finance.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
