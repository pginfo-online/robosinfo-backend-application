package com.ecommerce.marketplace.review.repository;

import com.ecommerce.marketplace.review.model.Review;
import com.ecommerce.marketplace.review.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByOrderItemId(UUID orderItemId);

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    List<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status);

    Page<Review> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Review> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId AND r.status = 'APPROVED'")
    Double getAverageRatingByProductId(UUID productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId AND r.status = 'APPROVED'")
    Long countByProductId(UUID productId);
}
