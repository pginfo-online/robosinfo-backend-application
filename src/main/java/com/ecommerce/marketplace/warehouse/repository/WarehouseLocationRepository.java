package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, UUID> {
    Optional<WarehouseLocation> findByBarcode(String barcode);
    List<WarehouseLocation> findByWarehouseId(UUID warehouseId);
    List<WarehouseLocation> findByZoneId(UUID zoneId);
}
