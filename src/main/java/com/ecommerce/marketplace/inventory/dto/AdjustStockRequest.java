package com.ecommerce.marketplace.inventory.dto;

import com.ecommerce.marketplace.inventory.model.InventoryEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustStockRequest {

    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    private UUID warehouseId;

    @NotNull(message = "Quantity change is required")
    private Integer qtyChange;

    @Builder.Default
    private InventoryEventType eventType = InventoryEventType.ADJUSTMENT;

    private String reason;
}
