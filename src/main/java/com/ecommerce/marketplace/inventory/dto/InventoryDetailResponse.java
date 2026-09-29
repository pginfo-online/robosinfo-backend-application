package com.ecommerce.marketplace.inventory.dto;

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
public class InventoryDetailResponse {

    private UUID id;
    private UUID variantId;
    private UUID sellerId;
    private UUID warehouseId;
    private String sku;
    private String productTitle;
    private String primaryImageUrl;
    private Integer physicalQty;
    private Integer reservedQty;
    private Integer damagedQty;
    private Integer availableQty;
    private Integer safetyBuffer;
    private String stockStatus; // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private Instant updatedAt;
}
