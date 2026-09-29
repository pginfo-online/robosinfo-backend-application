package com.ecommerce.marketplace.warehouse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratePicklistRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private List<UUID> shipmentIds;
    private UUID assignedStaffId;
}
