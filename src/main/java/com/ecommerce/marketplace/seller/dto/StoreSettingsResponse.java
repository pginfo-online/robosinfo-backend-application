package com.ecommerce.marketplace.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSettingsResponse {

    private UUID sellerId;
    private String businessName;
    private String supportEmail;
    private String supportPhone;
    private String storeDescription;
    private String pickupAddress;
    private Integer returnPolicyDays;
    private String gstNumber;
    private String panNumber;
    private boolean kycVerified;
    private boolean bankAccountConfigured;
    private String maskedBankAccount;
}
