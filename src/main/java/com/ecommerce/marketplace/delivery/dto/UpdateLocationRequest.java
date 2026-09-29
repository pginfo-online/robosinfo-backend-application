package com.ecommerce.marketplace.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLocationRequest {

    private UUID shipmentId;

    @NotNull(message = "Latitude is required")
    private BigDecimal lat;

    @NotNull(message = "Longitude is required")
    private BigDecimal lng;

    private BigDecimal speed;
    private BigDecimal heading;
    private Integer batteryLevel;
}
