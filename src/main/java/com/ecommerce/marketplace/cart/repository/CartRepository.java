package com.ecommerce.marketplace.cart.repository;

import com.ecommerce.marketplace.cart.model.Cart;
import com.ecommerce.marketplace.cart.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerIdAndStatus(UUID customerId, CartStatus status);

    Optional<Cart> findByCustomerId(UUID customerId);
}
