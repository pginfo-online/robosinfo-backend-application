package com.ecommerce.marketplace.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
public class RefundRequest {

    @NotNull(message = "Payment intent ID is required")
    private UUID paymentIntentId;

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private UUID orderItemId;

    @NotNull(message = "Refund amount is required")
    @Min(value = 100, message = "Amount must be at least 100 paisa")
    private Long amountPaisa;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    @NotNull(message = "Idempotency key is required")
    private UUID idempotencyKey;
}
