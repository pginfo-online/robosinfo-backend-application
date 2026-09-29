package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.DispatchManifest;
import com.ecommerce.marketplace.warehouse.model.ManifestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispatchManifestRepository extends JpaRepository<DispatchManifest, UUID> {
    Optional<DispatchManifest> findByManifestNumber(String manifestNumber);
    Page<DispatchManifest> findByWarehouseId(UUID warehouseId, Pageable pageable);
    Page<DispatchManifest> findByWarehouseIdAndStatus(UUID warehouseId, ManifestStatus status, Pageable pageable);
}
