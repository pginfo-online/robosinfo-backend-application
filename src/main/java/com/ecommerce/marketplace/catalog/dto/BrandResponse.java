package com.ecommerce.marketplace.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponse {

    private UUID id;
    private String name;
    private String slug;
    private String logoUrl;
    private Boolean isActive;
    private Instant createdAt;
}
