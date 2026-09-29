package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProductCard {
    private UUID id;
    private String title;
    private String slug;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private Long minPricePaisa;
    private Long mrpPaisa;
    private String imageUrl;
    private Double averageRating;
    private Long totalReviews;
    private Boolean inStock;
    private Instant createdAt;
}
