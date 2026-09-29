package com.ecommerce.marketplace.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_settlements", schema = "finance_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SellerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "gross_amount_paisa", nullable = false)
    private Long grossAmountPaisa;

    @Column(name = "commission_paisa", nullable = false)
    private Long commissionPaisa;

    @Column(name = "fees_paisa", nullable = false)
    @Builder.Default
    private Long feesPaisa = 0L;

    @Column(name = "tax_on_commission_paisa", nullable = false)
    @Builder.Default
    private Long taxOnCommissionPaisa = 0L;

    @Column(name = "refund_deductions_paisa", nullable = false)
    @Builder.Default
    private Long refundDeductionsPaisa = 0L;

    @Column(name = "net_amount_paisa", nullable = false)
    private Long netAmountPaisa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
