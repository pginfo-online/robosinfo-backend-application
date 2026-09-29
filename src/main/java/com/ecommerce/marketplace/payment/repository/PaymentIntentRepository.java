package com.ecommerce.marketplace.payment.repository;

import com.ecommerce.marketplace.payment.model.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    Optional<PaymentIntent> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentIntent> findByIdempotencyKey(UUID idempotencyKey);

    List<PaymentIntent> findByOrderId(UUID orderId);
}
