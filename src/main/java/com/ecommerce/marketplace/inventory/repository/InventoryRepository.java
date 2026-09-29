package com.ecommerce.marketplace.inventory.repository;

import com.ecommerce.marketplace.inventory.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByVariantIdAndSellerId(UUID variantId, UUID sellerId);

    List<Inventory> findByVariantId(UUID variantId);

    List<Inventory> findBySellerId(UUID sellerId);

    Page<Inventory> findBySellerId(UUID sellerId, Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variantId = :variantId AND i.sellerId = :sellerId")
    Optional<Inventory> findByVariantIdAndSellerIdForUpdate(
        @Param("variantId") UUID variantId,
        @Param("sellerId") UUID sellerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") UUID id);
}
