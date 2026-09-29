package com.ecommerce.marketplace.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Variant attributes JSON is required")
    private String variantAttributes;

    private Integer weightGrams;
    private String dimensionsCm;
}
