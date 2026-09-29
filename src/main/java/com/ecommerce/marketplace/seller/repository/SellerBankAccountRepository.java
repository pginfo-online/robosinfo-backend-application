package com.ecommerce.marketplace.seller.repository;

import com.ecommerce.marketplace.seller.model.SellerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, UUID> {

    List<SellerBankAccount> findBySellerId(UUID sellerId);

    Optional<SellerBankAccount> findBySellerIdAndIsPrimaryTrue(UUID sellerId);

    @Modifying
    @Query("UPDATE SellerBankAccount b SET b.isPrimary = false WHERE b.sellerId = :sellerId")
    void resetPrimaryBankAccounts(@Param("sellerId") UUID sellerId);
}
