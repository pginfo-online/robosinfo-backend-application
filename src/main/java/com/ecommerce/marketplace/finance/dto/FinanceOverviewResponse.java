package com.ecommerce.marketplace.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinanceOverviewResponse {

    private Long grossSalesPaisa;
    private Long totalCommissionPaisa;
    private Long totalTaxPaisa;
    private Long settledThisMonthPaisa;
    private Long upcomingPayoutPaisa;
    private LocalDate upcomingPayoutDate;
    private Integer pendingSettlementsCount;
    private Integer totalSettlementsCount;
}
