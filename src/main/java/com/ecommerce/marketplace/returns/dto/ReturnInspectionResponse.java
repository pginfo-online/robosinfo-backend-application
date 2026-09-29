package com.ecommerce.marketplace.returns.dto;

import com.ecommerce.marketplace.returns.model.ReturnDisposition;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnInspectionResponse {
    private UUID id;
    private UUID warehouseId;
    private UUID inspectedBy;
    private Boolean qcPassed;
    private ReturnDisposition disposition;
    private String inspectorNotes;
    private String defectDescription;
    private Instant inspectedAt;
}
