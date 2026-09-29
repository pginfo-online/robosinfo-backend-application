package com.ecommerce.marketplace.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "warehouse_locations", schema = "warehouse_schema",
       uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "aisle", "shelf", "bin"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private WarehouseZone zone;

    @Column(nullable = false, length = 20)
    private String aisle;

    @Column(nullable = false, length = 20)
    private String shelf;

    @Column(nullable = false, length = 20)
    private String bin;

    @Column(nullable = false, unique = true, length = 100)
    private String barcode;

    @Column(name = "max_weight_grams")
    private Integer maxWeightGrams;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
