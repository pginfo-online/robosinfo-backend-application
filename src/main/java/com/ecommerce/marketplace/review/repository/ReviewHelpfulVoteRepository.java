package com.ecommerce.marketplace.review.repository;

import com.ecommerce.marketplace.review.model.ReviewHelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, UUID> {
    Optional<ReviewHelpfulVote> findByReviewIdAndUserId(UUID reviewId, UUID userId);
}
