package com.ecommerce.marketplace.review.dto;

import com.ecommerce.marketplace.review.model.ReviewStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID customerId;
    private UUID productId;
    private UUID variantId;
    private UUID orderId;
    private UUID orderItemId;
    private Integer rating;
    private String title;
    private String comment;
    private String photoUrls;
    private Boolean isVerifiedBuyer;
    private ReviewStatus status;
    private Integer helpfulVotes;
    private UUID sellerId;
    private String sellerResponse;
    private Instant sellerRespondedAt;
    private Instant createdAt;
}
