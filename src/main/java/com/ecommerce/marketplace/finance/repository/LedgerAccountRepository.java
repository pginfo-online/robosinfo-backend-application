package com.ecommerce.marketplace.finance.repository;

import com.ecommerce.marketplace.finance.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByName(String name);

    Optional<LedgerAccount> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
