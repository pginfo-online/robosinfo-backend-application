package com.ecommerce.marketplace.admin.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMetricsOverviewResponse {
    private Long gmvPaisa;
    private Long totalOrders;
    private Map<String, Long> ordersByStatus;
    private Long totalSellers;
    private Long verifiedSellers;
    private Long pendingSellers;
    private Long totalProducts;
    private Long approvedProducts;
    private Long pendingProducts;
    private Long totalCustomers;
    private Long openTickets;
    private Long escalatedTickets;
    private Long totalReturns;
}
