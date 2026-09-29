package com.ecommerce.marketplace.review.service;

import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.review.dto.*;
import com.ecommerce.marketplace.review.model.Review;
import com.ecommerce.marketplace.review.model.ReviewHelpfulVote;
import com.ecommerce.marketplace.review.model.ReviewStatus;
import com.ecommerce.marketplace.review.repository.ReviewHelpfulVoteRepository;
import com.ecommerce.marketplace.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReviewResponse createReview(UUID customerId, CreateReviewRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId().toString()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessRuleException("Order does not belong to customer", "FORBIDDEN");
        }

        // Verified Buyer check: order must be DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Only delivered orders can be reviewed", "ORDER_NOT_DELIVERED");
        }

        OrderItem item = order.getItems().stream()
            .filter(i -> i.getId().equals(request.getOrderItemId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Order Item", request.getOrderItemId().toString()));

        // Check duplicate review
        if (reviewRepository.findByOrderItemId(item.getId()).isPresent()) {
            throw new BusinessRuleException("You have already reviewed this item", "DUPLICATE_REVIEW");
        }

        // Find parent product ID
        UUID productId = null;
        for (Product p : productRepository.findAll()) {
            if (p.getVariants().stream().anyMatch(v -> v.getId().equals(item.getVariantId()))) {
                productId = p.getId();
                break;
            }
        }
        if (productId == null) {
            productId = item.getVariantId(); // fallback
        }

        String photosJson = null;
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            try {
                photosJson = objectMapper.writeValueAsString(request.getPhotoUrls());
            } catch (Exception e) {
                photosJson = "[]";
            }
        }

        Review review = Review.builder()
            .customerId(customerId)
            .productId(productId)
            .variantId(item.getVariantId())
            .orderId(order.getId())
            .orderItemId(item.getId())
            .rating(request.getRating())
            .title(request.getTitle())
            .comment(request.getComment())
            .photoUrls(photosJson)
            .isVerifiedBuyer(true)
            .status(ReviewStatus.APPROVED)
            .helpfulVotes(0)
            .sellerId(item.getSellerId())
            .build();

        review = reviewRepository.save(review);
        log.info("Created verified buyer review {} for product {}", review.getId(), productId);
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse voteHelpful(UUID userId, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));

        if (helpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId).isPresent()) {
            throw new BusinessRuleException("You have already voted this review as helpful", "ALREADY_VOTED");
        }

        ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
            .reviewId(reviewId)
            .userId(userId)
            .build();
        helpfulVoteRepository.save(vote);

        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        review = reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional
    public ReviewResponse addSellerResponse(UUID sellerId, UUID reviewId, String response) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));

        if (review.getSellerId() == null || !review.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("Only the seller of this product can respond to this review", "FORBIDDEN");
        }

        review.setSellerResponse(response);
        review.setSellerRespondedAt(Instant.now());
        review = reviewRepository.save(review);
        log.info("Seller {} responded to review {}", sellerId, reviewId);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable);
        List<ReviewResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getProductRatingSummary(UUID productId) {
        List<Review> approved = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED);

        long total = approved.size();
        double avg = total > 0
            ? approved.stream().mapToInt(Review::getRating).average().orElse(0.0)
            : 0.0;

        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            starCounts.put(i, 0L);
        }
        for (Review r : approved) {
            starCounts.put(r.getRating(), starCounts.getOrDefault(r.getRating(), 0L) + 1);
        }

        // Round average to 1 decimal place
        double roundedAvg = Math.round(avg * 10.0) / 10.0;

        return ProductRatingSummaryResponse.builder()
            .productId(productId)
            .averageRating(roundedAvg)
            .totalReviews(total)
            .starCounts(starCounts)
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getCustomerReviews(UUID customerId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByCustomerId(customerId, pageable);
        List<ReviewResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    public ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
            .id(r.getId())
            .customerId(r.getCustomerId())
            .productId(r.getProductId())
            .variantId(r.getVariantId())
            .orderId(r.getOrderId())
            .orderItemId(r.getOrderItemId())
            .rating(r.getRating())
            .title(r.getTitle())
            .comment(r.getComment())
            .photoUrls(r.getPhotoUrls())
            .isVerifiedBuyer(r.getIsVerifiedBuyer())
            .status(r.getStatus())
            .helpfulVotes(r.getHelpfulVotes())
            .sellerId(r.getSellerId())
            .sellerResponse(r.getSellerResponse())
            .sellerRespondedAt(r.getSellerRespondedAt())
            .createdAt(r.getCreatedAt())
            .build();
    }
}
