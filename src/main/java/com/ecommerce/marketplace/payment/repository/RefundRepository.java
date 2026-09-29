package com.ecommerce.marketplace.payment.repository;

import com.ecommerce.marketplace.payment.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByOrderId(UUID orderId);

    Optional<Refund> findByIdempotencyKey(UUID idempotencyKey);
}
