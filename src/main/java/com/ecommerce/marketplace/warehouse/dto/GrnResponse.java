package com.ecommerce.marketplace.warehouse.dto;

import com.ecommerce.marketplace.warehouse.model.GrnStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrnResponse {
    private UUID id;
    private String grnNumber;
    private UUID warehouseId;
    private UUID sellerId;
    private GrnStatus status;
    private String consignmentReference;
    private UUID receivedBy;
    private Instant receivedAt;
    private String notes;
    private List<GrnItemResponse> items;
    private Instant createdAt;
}
