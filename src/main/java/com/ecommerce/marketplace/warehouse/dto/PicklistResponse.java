package com.ecommerce.marketplace.warehouse.dto;

import com.ecommerce.marketplace.warehouse.model.PicklistStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PicklistResponse {
    private UUID id;
    private String picklistNumber;
    private UUID warehouseId;
    private PicklistStatus status;
    private UUID assignedStaffId;
    private List<PicklistItemResponse> items;
    private Instant createdAt;
}
