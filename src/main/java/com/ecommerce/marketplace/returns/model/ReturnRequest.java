package com.ecommerce.marketplace.returns.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "return_requests", schema = "order_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "return_number", nullable = false, unique = true, length = 100)
    private String returnNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false, length = 30)
    @Builder.Default
    private ReturnType returnType = ReturnType.REFUND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnReason reason;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "photo_urls", columnDefinition = "jsonb")
    private String photoUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(name = "pickup_address_snapshot", columnDefinition = "jsonb")
    private String pickupAddressSnapshot;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @OneToOne(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnInspection inspection;
}
