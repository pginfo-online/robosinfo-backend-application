package com.ecommerce.marketplace.warehouse.model;

import com.ecommerce.marketplace.delivery.model.Shipment;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "picklist_items", schema = "warehouse_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PicklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picklist_id", nullable = false)
    private Picklist picklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "qty_to_pick", nullable = false)
    private Integer qtyToPick;

    @Column(name = "qty_picked", nullable = false)
    @Builder.Default
    private Integer qtyPicked = 0;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
}
