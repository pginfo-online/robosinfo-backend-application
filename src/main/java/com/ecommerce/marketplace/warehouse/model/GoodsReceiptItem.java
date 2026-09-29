package com.ecommerce.marketplace.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "goods_receipt_items", schema = "warehouse_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceiptNote grn;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "expected_qty", nullable = false)
    @Builder.Default
    private Integer expectedQty = 0;

    @Column(name = "received_qty", nullable = false)
    @Builder.Default
    private Integer receivedQty = 0;

    @Column(name = "passed_qty", nullable = false)
    @Builder.Default
    private Integer passedQty = 0;

    @Column(name = "failed_qty", nullable = false)
    @Builder.Default
    private Integer failedQty = 0;

    @Column(name = "damaged_qty", nullable = false)
    @Builder.Default
    private Integer damagedQty = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ItemDisposition disposition = ItemDisposition.ACCEPTED;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
