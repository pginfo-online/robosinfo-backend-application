package com.ecommerce.marketplace.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerSalesAnalyticsResponse {

    private Long totalRevenuePaisa;
    private Integer totalOrders;
    private Double averageOrderValuePaisa;
    private Double returnRatePercent;
    private List<SalesDataPoint> timeline;
    private List<TopProductMetric> topProducts;
    private List<CategoryRevenueMetric> categoryDistribution;
    private RatingBreakdown ratingBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesDataPoint {
        private String date;
        private Long salesPaisa;
        private Integer ordersCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductMetric {
        private UUID productId;
        private UUID variantId;
        private String title;
        private String sku;
        private Integer unitsSold;
        private Long revenuePaisa;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryRevenueMetric {
        private String categoryName;
        private Long revenuePaisa;
        private Double sharePercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatingBreakdown {
        private Integer fiveStar;
        private Integer fourStar;
        private Integer threeStar;
        private Integer twoStar;
        private Integer oneStar;
        private Double averageRating;
        private Integer totalReviews;
    }
}
