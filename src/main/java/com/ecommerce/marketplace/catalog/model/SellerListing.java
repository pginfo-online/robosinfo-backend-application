package com.ecommerce.marketplace.catalog.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_listings", schema = "catalog_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SellerListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "mrp_paisa", nullable = false)
    private Long mrpPaisa;

    @Column(name = "selling_price_paisa", nullable = false)
    private Long sellingPricePaisa;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ConditionType condition = ConditionType.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", length = 20)
    @Builder.Default
    private FulfillmentType fulfillmentType = FulfillmentType.MARKETPLACE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
