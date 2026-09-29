package com.ecommerce.marketplace.payment.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.payment.dto.*;
import com.ecommerce.marketplace.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments & Checkout", description = "Endpoints for initiating, verifying Razorpay payments, and requesting refunds")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent")
    @Operation(summary = "Create Razorpay payment intent for an order (idempotent)")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request) {
        PaymentIntentResponse response = paymentService.createPaymentIntent(request);
        return ResponseEntity.ok(ApiResponse.success("Payment intent created", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature and advance order to PAID")
    public ResponseEntity<ApiResponse<Boolean>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        boolean verified = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", verified));
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Initiate refund for an order item or whole payment (Admin only)")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RefundRequest request) {
        RefundResponse response = paymentService.createRefund(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated", response));
    }
}
