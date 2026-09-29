package com.ecommerce.marketplace.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "order_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "seller_listing_id", nullable = false)
    private UUID sellerListingId;

    @Column(name = "product_snapshot", nullable = false, columnDefinition = "jsonb")
    private String productSnapshot;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "unit_price_paisa", nullable = false)
    private Long unitPricePaisa;

    @Column(name = "mrp_paisa", nullable = false)
    private Long mrpPaisa;

    @Column(name = "discount_paisa", nullable = false)
    @Builder.Default
    private Long discountPaisa = 0L;

    @Column(name = "tax_paisa", nullable = false)
    private Long taxPaisa;

    @Column(name = "total_paisa", nullable = false)
    private Long totalPaisa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.CREATED;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "fulfillment_warehouse_id")
    private UUID fulfillmentWarehouseId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
