package com.ecommerce.marketplace.promotion.dto;

import com.ecommerce.marketplace.promotion.model.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    private Long discountValue;

    @Builder.Default
    private Long minOrderValuePaisa = 0L;
    private Long maxDiscountCapPaisa;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotNull(message = "End date is required")
    private Instant endDate;

    @Builder.Default
    private Integer usageLimitPerUser = 1;
    @Builder.Default
    private Integer totalUsageLimit = 1000;
}
