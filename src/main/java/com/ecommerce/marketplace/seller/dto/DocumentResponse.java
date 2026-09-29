package com.ecommerce.marketplace.seller.dto;

import com.ecommerce.marketplace.seller.model.DocumentStatus;
import com.ecommerce.marketplace.seller.model.DocumentType;
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
public class DocumentResponse {

    private UUID id;
    private DocumentType documentType;
    private String fileUrl;
    private DocumentStatus status;
    private String rejectionReason;
    private Instant uploadedAt;
    private Instant verifiedAt;
}
