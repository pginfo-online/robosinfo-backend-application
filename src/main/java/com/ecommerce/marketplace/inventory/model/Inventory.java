package com.ecommerce.marketplace.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory", schema = "inventory_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "physical_qty", nullable = false)
    @Builder.Default
    private Integer physicalQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    @Builder.Default
    private Integer reservedQty = 0;

    @Column(name = "damaged_qty", nullable = false)
    @Builder.Default
    private Integer damagedQty = 0;

    @Column(name = "safety_buffer")
    @Builder.Default
    private Integer safetyBuffer = 10;


    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public int getAvailableQty() {
        return physicalQty - reservedQty - damagedQty;
    }
}
