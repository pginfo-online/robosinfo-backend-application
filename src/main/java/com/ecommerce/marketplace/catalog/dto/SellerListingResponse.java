package com.ecommerce.marketplace.catalog.dto;

import com.ecommerce.marketplace.catalog.model.ConditionType;
import com.ecommerce.marketplace.catalog.model.FulfillmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerListingResponse {

    private UUID id;
    private UUID sellerId;
    private UUID variantId;
    private Long mrpPaisa;
    private Long sellingPricePaisa;
    private Boolean isActive;
    private ConditionType condition;
    private FulfillmentType fulfillmentType;
}
