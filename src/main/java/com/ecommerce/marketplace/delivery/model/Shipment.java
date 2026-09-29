package com.ecommerce.marketplace.delivery.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments", schema = "shipping_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "awb_number", nullable = false, unique = true, length = 100)
    private String awbNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "carrier_name", length = 100)
    @Builder.Default
    private String carrierName = "INTERNAL_FLEET";

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "delivery_partner_id")
    private UUID deliveryPartnerId;

    @Column(name = "delivery_otp", length = 10)
    private String deliveryOtp;

    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    @Column(name = "max_otp_attempts", nullable = false)
    @Builder.Default
    private Integer maxOtpAttempts = 3;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "shipping_address_snapshot", columnDefinition = "jsonb")
    private String shippingAddressSnapshot;

    @Column(name = "cod_amount_paisa")
    @Builder.Default
    private Long codAmountPaisa = 0L;

    @Column(name = "is_cod")
    @Builder.Default
    private Boolean isCod = false;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShipmentItem> items = new ArrayList<>();
}
