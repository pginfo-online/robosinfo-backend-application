package com.ecommerce.marketplace.payment.dto;

import com.ecommerce.marketplace.payment.model.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private UUID id;
    private UUID paymentIntentId;
    private UUID orderId;
    private Long amountPaisa;
    private RefundStatus status;
    private String razorpayRefundId;
}
