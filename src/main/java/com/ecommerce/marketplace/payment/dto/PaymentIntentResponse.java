package com.ecommerce.marketplace.payment.dto;

import com.ecommerce.marketplace.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {

    private UUID id;
    private UUID orderId;
    private Long amountPaisa;
    private String currency;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private PaymentStatus status;
}
