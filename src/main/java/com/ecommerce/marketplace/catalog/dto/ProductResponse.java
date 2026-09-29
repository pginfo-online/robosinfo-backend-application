package com.ecommerce.marketplace.catalog.dto;

import com.ecommerce.marketplace.catalog.model.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private UUID id;
    private String title;
    private String slug;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String attributes;
    private ProductStatus status;
    private UUID createdBy;
    private List<VariantResponse> variants;
    private List<String> imageUrls;
    private Double averageRating;
    private Long totalReviews;
    private Instant createdAt;
}
