package com.ecommerce.marketplace.promotion.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons", schema = "promotion_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue; // Paisa for FLAT, percentage (1..100) for PERCENTAGE

    @Column(name = "min_order_value_paisa", nullable = false)
    @Builder.Default
    private Long minOrderValuePaisa = 0L;

    @Column(name = "max_discount_cap_paisa")
    private Long maxDiscountCapPaisa;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "usage_limit_per_user", nullable = false)
    @Builder.Default
    private Integer usageLimitPerUser = 1;

    @Column(name = "total_usage_limit", nullable = false)
    @Builder.Default
    private Integer totalUsageLimit = 1000;

    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
