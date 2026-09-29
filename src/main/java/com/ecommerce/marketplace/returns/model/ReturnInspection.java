package com.ecommerce.marketplace.returns.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_inspections", schema = "order_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ReturnInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "inspected_by", nullable = false)
    private UUID inspectedBy;

    @Column(name = "qc_passed", nullable = false)
    private Boolean qcPassed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnDisposition disposition;

    @Column(name = "inspector_notes", columnDefinition = "TEXT")
    private String inspectorNotes;

    @Column(name = "defect_description", columnDefinition = "TEXT")
    private String defectDescription;

    @CreatedDate
    @Column(name = "inspected_at", nullable = false, updatable = false)
    private Instant inspectedAt;
}
