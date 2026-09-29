package com.ecommerce.marketplace.warehouse.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PicklistItemResponse {
    private UUID id;
    private UUID shipmentId;
    private UUID orderItemId;
    private UUID variantId;
    private UUID locationId;
    private Integer qtyToPick;
    private Integer qtyPicked;
    private Boolean isVerified;
}
