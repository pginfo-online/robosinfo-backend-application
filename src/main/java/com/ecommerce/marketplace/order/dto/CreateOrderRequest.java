package com.ecommerce.marketplace.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Delivery address ID is required")
    private UUID addressId;

    @NotNull(message = "Idempotency key is required")
    private UUID idempotencyKey;

    private String couponCode;
    private String notes;
}
