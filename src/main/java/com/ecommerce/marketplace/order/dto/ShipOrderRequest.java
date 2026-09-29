package com.ecommerce.marketplace.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipOrderRequest {

    @NotBlank(message = "Carrier name is required")
    private String carrierName;

    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;

    private String trackingUrl;

    private String notes;
}
