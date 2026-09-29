package com.ecommerce.marketplace.finance.repository;

import com.ecommerce.marketplace.finance.model.SellerSettlement;
import com.ecommerce.marketplace.finance.model.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SellerSettlementRepository extends JpaRepository<SellerSettlement, UUID> {

    List<SellerSettlement> findBySellerIdOrderByPeriodEndDesc(UUID sellerId);

    Page<SellerSettlement> findBySellerIdOrderByPeriodEndDesc(UUID sellerId, Pageable pageable);

    Page<SellerSettlement> findByStatus(SettlementStatus status, Pageable pageable);

}
