package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.Picklist;
import com.ecommerce.marketplace.warehouse.model.PicklistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PicklistRepository extends JpaRepository<Picklist, UUID> {
    Optional<Picklist> findByPicklistNumber(String picklistNumber);
    Page<Picklist> findByWarehouseId(UUID warehouseId, Pageable pageable);
    Page<Picklist> findByWarehouseIdAndStatus(UUID warehouseId, PicklistStatus status, Pageable pageable);
}
