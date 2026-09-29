package com.ecommerce.marketplace.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanItemRequest {

    @NotNull(message = "Picklist item ID is required")
    private UUID picklistItemId;

    @NotBlank(message = "Barcode or SKU is required")
    private String barcode;
}
