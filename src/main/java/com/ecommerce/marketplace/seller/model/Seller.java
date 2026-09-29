package com.ecommerce.marketplace.seller.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sellers", schema = "seller_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 50)
    private BusinessType businessType;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SellerStatus status = SellerStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 30)
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.INCOMPLETE;

    @Column(name = "commission_rate_bps", nullable = false)
    @Builder.Default
    private Integer commissionRateBps = 1000; // 10.00%

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_orders")
    @Builder.Default
    private Long totalOrders = 0L;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "support_phone")
    private String supportPhone;

    @Column(name = "store_description", length = 1000)
    private String storeDescription;

    @Column(name = "pickup_address", length = 500)
    private String pickupAddress;

    @Column(name = "return_policy_days")
    @Builder.Default
    private Integer returnPolicyDays = 7;
}
