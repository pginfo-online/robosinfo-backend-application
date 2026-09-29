package com.ecommerce.marketplace.delivery.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemResponse {
    private UUID id;
    private UUID orderItemId;
    private UUID variantId;
    private Integer qty;
}
