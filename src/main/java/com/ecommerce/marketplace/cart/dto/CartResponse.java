package com.ecommerce.marketplace.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private UUID cartId;
    private UUID customerId;
    private List<CartItemResponse> items;
    private Long subtotalPaisa;
    private Long totalSavingsPaisa;
    private Integer itemCount;
}
