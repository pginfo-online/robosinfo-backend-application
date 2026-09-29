package com.ecommerce.marketplace.warehouse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePackingSlipRequest {

    @NotNull(message = "Shipment ID is required")
    private UUID shipmentId;

    @NotNull(message = "Package weight in grams is required")
    private Integer packageWeightGrams;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private String boxType;
}
