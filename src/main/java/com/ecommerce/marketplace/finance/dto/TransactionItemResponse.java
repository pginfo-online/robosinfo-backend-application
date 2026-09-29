package com.ecommerce.marketplace.finance.dto;

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
public class TransactionItemResponse {

    private UUID id;
    private String orderNumber;
    private String productTitle;
    private Long grossAmountPaisa;
    private Integer referralFeePercent;
    private Long commissionPaisa;
    private Long fixedFeePaisa;
    private Long gstOnFeePaisa;
    private Long tdsPaisa;
    private Long netCreditPaisa;
    private String status;
    private Instant createdAt;
}
