package com.ecommerce.marketplace.warehouse.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackingSlipResponse {
    private UUID id;
    private String slipNumber;
    private UUID shipmentId;
    private Integer packageWeightGrams;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private String boxType;
    private UUID packedBy;
    private Instant packedAt;
}
