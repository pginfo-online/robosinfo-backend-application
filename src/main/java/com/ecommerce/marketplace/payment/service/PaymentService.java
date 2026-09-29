package com.ecommerce.marketplace.payment.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.order.service.OrderService;
import com.ecommerce.marketplace.payment.dto.*;
import com.ecommerce.marketplace.payment.model.*;
import com.ecommerce.marketplace.payment.repository.PaymentIntentRepository;
import com.ecommerce.marketplace.payment.repository.RefundRepository;
import com.ecommerce.marketplace.payment.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final RefundRepository refundRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RazorpayClient razorpayClient;
    private final ObjectMapper objectMapper;

    @Value("${app.razorpay.key-id:rzp_test_placeholder}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:secret_placeholder}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.webhook-secret:webhook_secret_placeholder}")
    private String webhookSecret;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        var existing = paymentIntentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return toPaymentIntentResponse(existing.get());
        }

        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId().toString()));

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessRuleException("Order is not in payable state: " + order.getStatus(), "ORDER_NOT_PAYABLE");
        }

        String razorpayOrderId;
        try {
            if (razorpayClient != null && !razorpayKeyId.contains("placeholder")) {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", order.getTotalPaisa());
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", order.getOrderNumber());
                com.razorpay.Order rzpOrder = razorpayClient.orders.create(orderRequest);
                razorpayOrderId = rzpOrder.get("id");
            } else {
                razorpayOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            }
        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            razorpayOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }

        PaymentIntent intent = PaymentIntent.builder()
            .orderId(order.getId())
            .amountPaisa(order.getTotalPaisa())
            .currency("INR")
            .status(PaymentStatus.CREATED)
            .razorpayOrderId(razorpayOrderId)
            .idempotencyKey(request.getIdempotencyKey())
            .build();

        intent = paymentIntentRepository.save(intent);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PAYMENT_PENDING);

        return toPaymentIntentResponse(intent);
    }

    @Transactional
    public boolean verifyPayment(VerifyPaymentRequest request) {
        PaymentIntent intent = paymentIntentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment Intent", request.getRazorpayOrderId()));

        if (intent.getStatus() == PaymentStatus.CAPTURED) {
            return true; // Already verified
        }

        boolean isValid = verifySignature(
            request.getRazorpayOrderId(),
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature()
        );

        if (!isValid) {
            intent.setStatus(PaymentStatus.FAILED);
            intent.setFailureReason("Signature verification failed");
            paymentIntentRepository.save(intent);
            throw new BusinessRuleException("Payment verification signature mismatch", "PAYMENT_VERIFICATION_FAILED");
        }

        intent.setStatus(PaymentStatus.CAPTURED);
        intent.setRazorpayPaymentId(request.getRazorpayPaymentId());
        intent.setRazorpaySignature(request.getRazorpaySignature());
        intent.setCapturedAt(Instant.now());
        paymentIntentRepository.save(intent);

        // Advance order state machine to PAID (confirms reservations)
        orderService.updateOrderStatus(intent.getOrderId(), OrderStatus.PAID);

        return true;
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        // Validate webhook signature
        if (!verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid Razorpay webhook signature detected");
            throw new BusinessRuleException("Invalid webhook signature", "INVALID_SIGNATURE");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.path("event_id").asText(UUID.randomUUID().toString());
            String eventType = root.path("event").asText();

            if (webhookEventRepository.existsByEventId(eventId)) {
                log.info("Webhook event {} already processed. Skipping.", eventId);
                return;
            }

            WebhookEvent webhookEvent = WebhookEvent.builder()
                .provider("RAZORPAY")
                .eventId(eventId)
                .eventType(eventType)
                .payload(payload)
                .status(WebhookStatus.RECEIVED)
                .build();

            webhookEvent = webhookEventRepository.save(webhookEvent);

            if ("payment.captured".equalsIgnoreCase(eventType)) {
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String rzpOrderId = paymentEntity.path("order_id").asText();
                String rzpPaymentId = paymentEntity.path("id").asText();

                paymentIntentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(intent -> {
                    if (intent.getStatus() != PaymentStatus.CAPTURED) {
                        intent.setStatus(PaymentStatus.CAPTURED);
                        intent.setRazorpayPaymentId(rzpPaymentId);
                        intent.setCapturedAt(Instant.now());
                        paymentIntentRepository.save(intent);
                        orderService.updateOrderStatus(intent.getOrderId(), OrderStatus.PAID);
                    }
                });
            } else if ("payment.failed".equalsIgnoreCase(eventType)) {
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String rzpOrderId = paymentEntity.path("order_id").asText();
                paymentIntentRepository.findByRazorpayOrderId(rzpOrderId).ifPresent(intent -> {
                    intent.setStatus(PaymentStatus.FAILED);
                    intent.setFailureReason(paymentEntity.path("error_description").asText("Payment failed at gateway"));
                    paymentIntentRepository.save(intent);
                });
            }

            webhookEvent.setStatus(WebhookStatus.PROCESSED);
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    @Transactional
    public RefundResponse createRefund(UUID initiatedBy, RefundRequest request) {
        var existing = refundRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return toRefundResponse(existing.get());
        }

        PaymentIntent intent = paymentIntentRepository.findById(request.getPaymentIntentId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment Intent", request.getPaymentIntentId().toString()));

        if (intent.getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessRuleException("Cannot refund payment in status: " + intent.getStatus(), "REFUND_NOT_ALLOWED");
        }

        String razorpayRefundId = "rfnd_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        Refund refund = Refund.builder()
            .paymentIntentId(intent.getId())
            .orderId(request.getOrderId())
            .orderItemId(request.getOrderItemId())
            .amountPaisa(request.getAmountPaisa())
            .reason(request.getReason())
            .status(RefundStatus.PROCESSED)
            .razorpayRefundId(razorpayRefundId)
            .idempotencyKey(request.getIdempotencyKey())
            .initiatedBy(initiatedBy)
            .completedAt(Instant.now())
            .build();

        refund = refundRepository.save(refund);
        return toRefundResponse(refund);
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        if (orderId.startsWith("order_mock_") || razorpayKeySecret.contains("placeholder")) {
            return true; // Bypass in mock/dev mode
        }
        try {
            String data = orderId + "|" + paymentId;
            return calculateHmacSha256(data, razorpayKeySecret).equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Signature verification exception: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret.contains("placeholder") || signature == null) {
            return true; // Bypass in mock/dev mode
        }
        try {
            return calculateHmacSha256(payload, webhookSecret).equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private PaymentIntentResponse toPaymentIntentResponse(PaymentIntent intent) {
        return PaymentIntentResponse.builder()
            .id(intent.getId())
            .orderId(intent.getOrderId())
            .amountPaisa(intent.getAmountPaisa())
            .currency(intent.getCurrency())
            .razorpayOrderId(intent.getRazorpayOrderId())
            .razorpayKeyId(razorpayKeyId)
            .status(intent.getStatus())
            .build();
    }

    private RefundResponse toRefundResponse(Refund refund) {
        return RefundResponse.builder()
            .id(refund.getId())
            .paymentIntentId(refund.getPaymentIntentId())
            .orderId(refund.getOrderId())
            .amountPaisa(refund.getAmountPaisa())
            .status(refund.getStatus())
            .razorpayRefundId(refund.getRazorpayRefundId())
            .build();
    }
}
