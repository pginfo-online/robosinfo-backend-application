package com.ecommerce.marketplace.delivery.repository;

import com.ecommerce.marketplace.delivery.model.Shipment;
import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    List<Shipment> findByOrderId(UUID orderId);

    Optional<Shipment> findByAwbNumber(String awbNumber);

    List<Shipment> findByDeliveryPartnerId(UUID deliveryPartnerId);

    Page<Shipment> findByDeliveryPartnerIdAndStatus(UUID deliveryPartnerId, ShipmentStatus status, Pageable pageable);

    Page<Shipment> findByDeliveryPartnerId(UUID deliveryPartnerId, Pageable pageable);

    Page<Shipment> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
}
