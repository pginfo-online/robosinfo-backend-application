package com.ecommerce.marketplace.inventory.service;

import com.ecommerce.marketplace.inventory.model.InventoryReservation;
import com.ecommerce.marketplace.inventory.model.ReservationStatus;
import com.ecommerce.marketplace.inventory.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    @Scheduled(fixedDelayString = "${app.inventory.reservation-cleanup-interval-ms:30000}")
    @SchedulerLock(name = "cleanupExpiredReservations", lockAtMostFor = "25s", lockAtLeastFor = "5s")
    public void cleanupExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository
            .findByStatusAndExpiresAtBefore(ReservationStatus.HELD, Instant.now());

        if (!expired.isEmpty()) {
            log.info("Found {} expired inventory reservations to release", expired.size());
            for (InventoryReservation res : expired) {
                try {
                    inventoryService.expireReservation(res.getReservationKey());
                } catch (Exception e) {
                    log.error("Failed to release expired reservation {}: {}", res.getReservationKey(), e.getMessage());
                }
            }
        }
    }
}
