package com.ecommerce.marketplace.returns.dto;

import com.ecommerce.marketplace.returns.model.ReturnReason;
import com.ecommerce.marketplace.returns.model.ReturnType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReturnRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Order Item ID is required")
    private UUID orderItemId;

    @Builder.Default
    private ReturnType returnType = ReturnType.REFUND;

    @NotNull(message = "Return reason is required")
    private ReturnReason reason;

    private String customerNotes;
    private List<String> photoUrls;
}
