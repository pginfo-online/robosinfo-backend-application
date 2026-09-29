package com.ecommerce.marketplace.warehouse.dto;

import com.ecommerce.marketplace.warehouse.model.ItemDisposition;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrnItemResponse {
    private UUID id;
    private UUID variantId;
    private Integer expectedQty;
    private Integer receivedQty;
    private Integer passedQty;
    private Integer failedQty;
    private Integer damagedQty;
    private ItemDisposition disposition;
    private UUID locationId;
    private String notes;
}
