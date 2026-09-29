package com.ecommerce.marketplace.review.service;

import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.review.dto.CreateReviewRequest;
import com.ecommerce.marketplace.review.dto.ProductRatingSummaryResponse;
import com.ecommerce.marketplace.review.dto.ReviewResponse;
import com.ecommerce.marketplace.review.model.Review;
import com.ecommerce.marketplace.review.model.ReviewStatus;
import com.ecommerce.marketplace.review.repository.ReviewHelpfulVoteRepository;
import com.ecommerce.marketplace.review.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewHelpfulVoteRepository helpfulVoteRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReviewService reviewService;

    private UUID customerId;
    private UUID orderId;
    private UUID orderItemId;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

        orderItem = OrderItem.builder()
                .id(orderItemId)
                .variantId(UUID.randomUUID())
                .sellerId(UUID.randomUUID())
                .qty(1)
                .unitPricePaisa(200000L)
                .build();

        order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.DELIVERED)
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
    }

    @Test
    @DisplayName("Should create verified buyer review for delivered item")
    void testCreateVerifiedBuyerReview() {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(5)
                .title("Excellent build quality")
                .comment("Super fast delivery and pristine condition.")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(reviewRepository.findByOrderItemId(orderItemId)).thenReturn(Optional.empty());
        when(productRepository.findAll()).thenReturn(List.of());
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReviewResponse response = reviewService.createReview(customerId, request);

        assertNotNull(response);
        assertTrue(response.getIsVerifiedBuyer());
        assertEquals(5, response.getRating());
        assertEquals(ReviewStatus.APPROVED, response.getStatus());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Should reject review submission if order is not delivered")
    void testRejectIfNotDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        CreateReviewRequest request = CreateReviewRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .rating(4)
                .title("Good")
                .comment("Waiting for it")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                reviewService.createReview(customerId, request)
        );

        assertTrue(ex.getMessage().contains("Only delivered orders can be reviewed"));
    }

    @Test
    @DisplayName("Should calculate product rating aggregate and star breakdown correctly")
    void testProductRatingSummary() {
        UUID productId = UUID.randomUUID();
        List<Review> reviews = List.of(
                Review.builder().rating(5).status(ReviewStatus.APPROVED).build(),
                Review.builder().rating(5).status(ReviewStatus.APPROVED).build(),
                Review.builder().rating(4).status(ReviewStatus.APPROVED).build(),
                Review.builder().rating(2).status(ReviewStatus.APPROVED).build()
        );

        when(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED)).thenReturn(reviews);

        ProductRatingSummaryResponse summary = reviewService.getProductRatingSummary(productId);

        assertNotNull(summary);
        assertEquals(4, summary.getTotalReviews());
        // Average: (5 + 5 + 4 + 2) / 4 = 16 / 4 = 4.0
        assertEquals(4.0, summary.getAverageRating());
        assertEquals(2L, summary.getStarCounts().get(5));
        assertEquals(1L, summary.getStarCounts().get(4));
        assertEquals(1L, summary.getStarCounts().get(2));
        assertEquals(0L, summary.getStarCounts().get(1));
    }
}
