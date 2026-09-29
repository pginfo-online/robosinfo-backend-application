package com.ecommerce.marketplace.payment.controller;

import com.ecommerce.marketplace.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Payment Webhooks", description = "Public webhook receivers for payment gateways")
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay")
    @Operation(summary = "Receive and process Razorpay webhook notifications")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.info("Received Razorpay webhook callback");
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }
}
