package com.ecommerce.marketplace.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String variantAttributes;
    private Integer weightGrams;
    private String dimensionsCm;
    private Boolean isActive;
    private SellerListingResponse buyBoxListing;
}
