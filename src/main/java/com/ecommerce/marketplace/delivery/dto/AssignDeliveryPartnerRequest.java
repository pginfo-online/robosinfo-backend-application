package com.ecommerce.marketplace.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeliveryPartnerRequest {

    @NotNull(message = "Delivery partner ID is required")
    private UUID deliveryPartnerId;
}
