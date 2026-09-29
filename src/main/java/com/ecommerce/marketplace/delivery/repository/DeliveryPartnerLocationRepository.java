package com.ecommerce.marketplace.delivery.repository;

import com.ecommerce.marketplace.delivery.model.DeliveryPartnerLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryPartnerLocationRepository extends JpaRepository<DeliveryPartnerLocation, UUID> {

    Optional<DeliveryPartnerLocation> findFirstByShipmentIdOrderByRecordedAtDesc(UUID shipmentId);

    Optional<DeliveryPartnerLocation> findFirstByDeliveryPartnerIdOrderByRecordedAtDesc(UUID deliveryPartnerId);
}
