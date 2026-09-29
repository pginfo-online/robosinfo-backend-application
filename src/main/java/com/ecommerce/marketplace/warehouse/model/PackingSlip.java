package com.ecommerce.marketplace.warehouse.model;

import com.ecommerce.marketplace.delivery.model.Shipment;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "packing_slips", schema = "warehouse_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PackingSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slip_number", nullable = false, unique = true, length = 100)
    private String slipNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "package_weight_grams", nullable = false)
    private Integer packageWeightGrams;

    @Column(name = "length_cm", precision = 6, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 6, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "box_type", length = 50)
    private String boxType;

    @Column(name = "packed_by", nullable = false)
    private UUID packedBy;

    @CreatedDate
    @Column(name = "packed_at", nullable = false, updatable = false)
    private Instant packedAt;
}
