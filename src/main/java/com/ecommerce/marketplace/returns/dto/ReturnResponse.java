package com.ecommerce.marketplace.returns.dto;

import com.ecommerce.marketplace.returns.model.ReturnReason;
import com.ecommerce.marketplace.returns.model.ReturnStatus;
import com.ecommerce.marketplace.returns.model.ReturnType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnResponse {
    private UUID id;
    private String returnNumber;
    private UUID customerId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID variantId;
    private UUID sellerId;
    private ReturnType returnType;
    private ReturnReason reason;
    private String customerNotes;
    private String photoUrls;
    private ReturnStatus status;
    private String pickupAddressSnapshot;
    private String rejectionReason;
    private UUID approvedBy;
    private ReturnInspectionResponse inspection;
    private Instant createdAt;
    private Instant updatedAt;
}
