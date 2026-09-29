package com.ecommerce.marketplace.warehouse.model;

import com.ecommerce.marketplace.common.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "picklists", schema = "warehouse_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Picklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "picklist_number", nullable = false, unique = true, length = 100)
    private String picklistNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PicklistStatus status = PicklistStatus.GENERATED;

    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @OneToMany(mappedBy = "picklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PicklistItem> items = new ArrayList<>();
}
