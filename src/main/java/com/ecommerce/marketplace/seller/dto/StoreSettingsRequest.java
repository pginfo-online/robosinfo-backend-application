package com.ecommerce.marketplace.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettingsRequest {

    private String businessName;
    private String supportEmail;
    private String supportPhone;
    private String storeDescription;
    private String pickupAddress;
    private Integer returnPolicyDays;
}
