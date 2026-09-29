package com.ecommerce.marketplace.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountResponse {

    private UUID id;
    private String accountHolder;
    private String maskedAccountNumber;
    private String ifscCode;
    private String bankName;
    private Boolean isVerified;
    private Boolean isPrimary;
    private Instant createdAt;
}
