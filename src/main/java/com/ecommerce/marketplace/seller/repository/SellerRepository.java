package com.ecommerce.marketplace.seller.repository;

import com.ecommerce.marketplace.seller.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByUserId(UUID userId);

    boolean existsByGstNumber(String gstNumber);

    org.springframework.data.domain.Page<Seller> findByStatus(com.ecommerce.marketplace.seller.model.SellerStatus status, org.springframework.data.domain.Pageable pageable);

    boolean existsByPanNumber(String panNumber);
}
