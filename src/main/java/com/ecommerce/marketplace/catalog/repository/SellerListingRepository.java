package com.ecommerce.marketplace.catalog.repository;

import com.ecommerce.marketplace.catalog.model.SellerListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerListingRepository extends JpaRepository<SellerListing, UUID> {

    List<SellerListing> findByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(UUID variantId);

    List<SellerListing> findBySellerId(UUID sellerId);

    Page<SellerListing> findBySellerId(UUID sellerId, Pageable pageable);

    Page<SellerListing> findBySellerIdAndIsActive(UUID sellerId, Boolean isActive, Pageable pageable);

    Optional<SellerListing> findBySellerIdAndVariantId(UUID sellerId, UUID variantId);

    // Buy Box algorithm: lowest active selling price for a variant
    Optional<SellerListing> findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(UUID variantId);
}

