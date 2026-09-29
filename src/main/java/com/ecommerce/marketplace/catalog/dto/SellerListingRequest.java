package com.ecommerce.marketplace.catalog.dto;

import com.ecommerce.marketplace.catalog.model.ConditionType;
import com.ecommerce.marketplace.catalog.model.FulfillmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerListingRequest {

    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    @NotNull(message = "MRP in paisa is required")
    @Min(value = 100, message = "MRP must be at least 100 paisa (Rs. 1)")
    private Long mrpPaisa;

    @NotNull(message = "Selling price in paisa is required")
    @Min(value = 100, message = "Selling price must be at least 100 paisa (Rs. 1)")
    private Long sellingPricePaisa;

    private ConditionType condition = ConditionType.NEW;
    private FulfillmentType fulfillmentType = FulfillmentType.MARKETPLACE;
}
