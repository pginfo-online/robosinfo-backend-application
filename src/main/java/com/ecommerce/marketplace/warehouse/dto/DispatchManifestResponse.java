package com.ecommerce.marketplace.warehouse.dto;

import com.ecommerce.marketplace.warehouse.model.ManifestStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchManifestResponse {
    private UUID id;
    private String manifestNumber;
    private UUID warehouseId;
    private String carrierName;
    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private ManifestStatus status;
    private Integer totalShipments;
    private Instant dispatchedAt;
    private Instant createdAt;
}
