package com.ecommerce.marketplace.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDashboardMetricsResponse {

    private Long todaySalesPaisa;
    private Long monthSalesPaisa;
    private Integer todayOrdersCount;
    private Integer pendingOrdersCount;
    private Integer lowStockAlertCount;
    private Integer totalActiveListings;
    private BigDecimal rating;
    private Double slaDispatchRatePercent;
    private Double averageOrderValuePaisa;
}
