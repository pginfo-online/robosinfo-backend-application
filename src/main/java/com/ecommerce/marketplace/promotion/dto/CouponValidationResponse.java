package com.ecommerce.marketplace.promotion.dto;

import com.ecommerce.marketplace.promotion.model.DiscountType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResponse {
    private boolean valid;
    private String code;
    private String description;
    private DiscountType discountType;
    private Long discountPaisa;
    private Long finalTotalPaisa;
    private String message;
}
