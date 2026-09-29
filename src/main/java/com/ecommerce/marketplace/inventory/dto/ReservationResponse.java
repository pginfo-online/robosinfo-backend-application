package com.ecommerce.marketplace.inventory.dto;

import com.ecommerce.marketplace.inventory.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private UUID reservationId;
    private UUID reservationKey;
    private UUID inventoryId;
    private Integer qty;
    private ReservationStatus status;
    private Instant expiresAt;
}
