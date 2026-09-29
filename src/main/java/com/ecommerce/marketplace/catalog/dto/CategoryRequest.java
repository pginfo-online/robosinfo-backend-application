package com.ecommerce.marketplace.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;
    private UUID parentId;
    private Integer displayOrder;
    private String imageUrl;
    private String attributesTemplate;
    private Integer returnWindowDays;
}
