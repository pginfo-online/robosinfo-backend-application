package com.ecommerce.marketplace.order.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "shipping_address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshot;

    @Column(name = "subtotal_paisa", nullable = false)
    private Long subtotalPaisa;

    @Column(name = "discount_paisa", nullable = false)
    @Builder.Default
    private Long discountPaisa = 0L;

    @Column(name = "tax_paisa", nullable = false)
    private Long taxPaisa;

    @Column(name = "shipping_paisa", nullable = false)
    private Long shippingPaisa;

    @Column(name = "total_paisa", nullable = false)
    private Long totalPaisa;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
