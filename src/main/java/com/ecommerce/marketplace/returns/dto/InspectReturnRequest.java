package com.ecommerce.marketplace.returns.dto;

import com.ecommerce.marketplace.returns.model.ReturnDisposition;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectReturnRequest {

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    @NotNull(message = "QC passed status is required")
    private Boolean qcPassed;

    @NotNull(message = "Disposition is required")
    private ReturnDisposition disposition;

    private String inspectorNotes;
    private String defectDescription;
}
