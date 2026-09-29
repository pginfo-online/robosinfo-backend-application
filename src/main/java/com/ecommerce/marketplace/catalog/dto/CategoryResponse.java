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
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
    private Integer level;
    private Integer displayOrder;
    private String imageUrl;
    private Boolean isActive;
    private String attributesTemplate;
    private Integer returnWindowDays;
    private Instant createdAt;
}
