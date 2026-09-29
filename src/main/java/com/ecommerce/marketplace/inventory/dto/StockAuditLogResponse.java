package com.ecommerce.marketplace.inventory.dto;

import com.ecommerce.marketplace.inventory.model.InventoryEventType;
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
public class StockAuditLogResponse {

    private UUID id;
    private UUID inventoryId;
    private UUID variantId;
    private String sku;
    private String productTitle;
    private InventoryEventType eventType;
    private Integer qtyChange;
    private String referenceType;
    private UUID referenceId;
    private String reason;
    private UUID actorId;
    private Instant createdAt;
}
