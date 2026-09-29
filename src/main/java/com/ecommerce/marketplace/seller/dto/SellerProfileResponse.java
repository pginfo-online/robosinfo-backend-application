package com.ecommerce.marketplace.seller.dto;

import com.ecommerce.marketplace.seller.model.BusinessType;
import com.ecommerce.marketplace.seller.model.OnboardingStatus;
import com.ecommerce.marketplace.seller.model.SellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileResponse {

    private UUID id;
    private UUID userId;
    private String businessName;
    private BusinessType businessType;
    private String gstNumber;
    private String panNumber;
    private SellerStatus status;
    private OnboardingStatus onboardingStatus;
    private Integer commissionRateBps;
    private BigDecimal rating;
    private Long totalOrders;
    private String email;
    private String phone;
}
