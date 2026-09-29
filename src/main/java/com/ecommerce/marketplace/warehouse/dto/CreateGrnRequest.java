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
public class CreateGrnRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotNull(message = "Seller ID is required")
    private UUID sellerId;

    private String consignmentReference;
    private String notes;
    private List<GrnItemRequest> items;
}
