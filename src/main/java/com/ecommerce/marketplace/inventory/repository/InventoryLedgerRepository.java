package com.ecommerce.marketplace.inventory.repository;

import com.ecommerce.marketplace.inventory.model.InventoryLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, UUID> {

    List<InventoryLedger> findByInventoryIdOrderByCreatedAtDesc(UUID inventoryId);

    Page<InventoryLedger> findByInventoryIdInOrderByCreatedAtDesc(List<UUID> inventoryIds, Pageable pageable);
}

