package com.ecommerce.marketplace.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStockResponse {

    private UUID id;
    private UUID variantId;
    private UUID sellerId;
    private UUID warehouseId;
    private Integer physicalQty;
    private Integer reservedQty;
    private Integer damagedQty;
    private Integer availableQty;
}
