package com.ecommerce.marketplace.finance.dto;

import com.ecommerce.marketplace.finance.model.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {

    private UUID id;
    private UUID sellerId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long grossSalesPaisa;
    private Long commissionPaisa;
    private Long taxDeductedPaisa;
    private Long netPayablePaisa;
    private SettlementStatus status;
    private String payoutReference;
    private Instant paidAt;
}
