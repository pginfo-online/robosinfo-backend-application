package com.ecommerce.marketplace.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;
    private String description;
    private UUID parentId;
    private Integer displayOrder;
    private String imageUrl;

    @DecimalMin(value = "0.0", message = "Commission rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Commission rate cannot exceed 100%")
    private BigDecimal commissionRatePercent;

    private Boolean isActive;
    private String attributesTemplate;
    private Integer returnWindowDays;
}
