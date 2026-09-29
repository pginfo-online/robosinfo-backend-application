package com.ecommerce.marketplace.identity.dto;

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
public class WishlistItemResponse {

    private UUID id;
    private UUID productId;
    private String title;
    private String slug;
    private String categoryName;
    private String brandName;
    private Long minPricePaisa;
    private Long mrpPaisa;
    private String imageUrl;
    private Double averageRating;
    private Long totalReviews;
    private Boolean inStock;
    private Instant addedAt;
}
