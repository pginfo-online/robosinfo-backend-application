package com.ecommerce.marketplace.promotion.repository;

import com.ecommerce.marketplace.promotion.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCouponIdAndCustomerId(UUID couponId, UUID customerId);
}
