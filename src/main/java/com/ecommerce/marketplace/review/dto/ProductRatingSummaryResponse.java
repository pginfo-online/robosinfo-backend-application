package com.ecommerce.marketplace.review.dto;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingSummaryResponse {
    private UUID productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> starCounts; // 1 to 5 stars
}
