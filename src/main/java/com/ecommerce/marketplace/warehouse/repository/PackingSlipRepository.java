package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.PackingSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackingSlipRepository extends JpaRepository<PackingSlip, UUID> {
    Optional<PackingSlip> findByShipmentId(UUID shipmentId);
    Optional<PackingSlip> findBySlipNumber(String slipNumber);
}
