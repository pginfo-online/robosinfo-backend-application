package com.ecommerce.marketplace.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product title is required")
    private String title;

    private String slug;

    private String description;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private UUID brandId;

    private String attributes;

    private List<VariantRequest> variants;
}
