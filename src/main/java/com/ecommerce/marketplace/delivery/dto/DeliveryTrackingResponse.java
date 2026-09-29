package com.ecommerce.marketplace.delivery.dto;

import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTrackingResponse {
    private UUID shipmentId;
    private String awbNumber;
    private ShipmentStatus status;
    private String carrierName;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private Instant lastLocationUpdate;
    private Instant estimatedDeliveryAt;
    private Instant deliveredAt;
}
