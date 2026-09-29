package com.ecommerce.marketplace.identity.repository;

import com.ecommerce.marketplace.identity.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<WishlistItem> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    boolean existsByCustomerIdAndProductId(UUID customerId, UUID productId);

    void deleteByCustomerIdAndProductId(UUID customerId, UUID productId);

    long countByCustomerId(UUID customerId);
}
