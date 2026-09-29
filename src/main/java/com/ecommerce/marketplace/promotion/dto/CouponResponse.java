package com.ecommerce.marketplace.promotion.dto;

import com.ecommerce.marketplace.promotion.model.DiscountType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {
    private UUID id;
    private String code;
    private String description;
    private DiscountType discountType;
    private Long discountValue;
    private Long minOrderValuePaisa;
    private Long maxDiscountCapPaisa;
    private Instant startDate;
    private Instant endDate;
    private Integer usageLimitPerUser;
    private Integer totalUsageLimit;
    private Integer timesUsed;
    private Boolean isActive;
    private Instant createdAt;
}
