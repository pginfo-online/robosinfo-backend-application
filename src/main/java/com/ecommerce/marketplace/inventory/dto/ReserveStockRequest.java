package com.ecommerce.marketplace.inventory.dto;

import jakarta.validation.constraints.Min;
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
public class ReserveStockRequest {

    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    @NotNull(message = "Seller ID is required")
    private UUID sellerId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer qty;

    private UUID orderId;
    private UUID cartId;
    private UUID reservationKey;
}
