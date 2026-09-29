package com.ecommerce.marketplace.catalog.dto;

import com.ecommerce.marketplace.catalog.model.ConditionType;
import com.ecommerce.marketplace.catalog.model.FulfillmentType;
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
public class SellerListingDetailResponse {

    private UUID id;
    private UUID sellerId;
    private UUID variantId;
    private UUID productId;
    private String productTitle;
    private String sku;
    private String brandName;
    private String categoryName;
    private String primaryImageUrl;
    private Long mrpPaisa;
    private Long sellingPricePaisa;
    private Integer availableStock;
    private Boolean isActive;
    private ConditionType condition;
    private FulfillmentType fulfillmentType;
    private Instant createdAt;
    private Instant updatedAt;
}
