package com.ecommerce.marketplace.cart.repository;

import com.ecommerce.marketplace.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndSellerListingId(UUID cartId, UUID sellerListingId);

    void deleteByCartId(UUID cartId);
}
