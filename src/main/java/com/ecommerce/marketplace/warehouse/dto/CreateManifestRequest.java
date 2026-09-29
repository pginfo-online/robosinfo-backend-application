package com.ecommerce.marketplace.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateManifestRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    private String vehicleNumber;
    private String driverName;
    private String driverPhone;

    private List<UUID> shipmentIds;
}
