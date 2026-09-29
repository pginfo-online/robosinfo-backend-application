package com.ecommerce.marketplace.warehouse.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {
    private UUID id;
    private String name;
    private String code;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String contactNumber;
    private String email;
    private Boolean isActive;
    private Instant createdAt;
}
