package com.ecommerce.marketplace.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {

    private String url;
    private String publicId;
    private String format;
    private Long sizeBytes;
}
