package com.ecommerce.marketplace.inventory.repository;

import com.ecommerce.marketplace.inventory.model.InventoryReservation;
import com.ecommerce.marketplace.inventory.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByReservationKey(UUID reservationKey);

    List<InventoryReservation> findByOrderId(UUID orderId);

    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant before);
}
