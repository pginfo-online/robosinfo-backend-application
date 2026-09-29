package com.ecommerce.marketplace.catalog.dto;

import com.ecommerce.marketplace.catalog.model.ConditionType;
import com.ecommerce.marketplace.catalog.model.FulfillmentType;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {

    @Positive(message = "MRP must be greater than zero")
    private Long mrpPaisa;

    @Positive(message = "Selling price must be greater than zero")
    private Long sellingPricePaisa;

    private Boolean isActive;

    private ConditionType condition;

    private FulfillmentType fulfillmentType;
}
