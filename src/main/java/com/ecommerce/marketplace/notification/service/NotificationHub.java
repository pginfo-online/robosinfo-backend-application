package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.notification.dto.NotificationResponse;
import com.ecommerce.marketplace.notification.model.*;
import com.ecommerce.marketplace.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationHub {

    private final PushNotificationService pushNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void dispatchOrderPlaced(UUID customerId, String customerEmail, String orderNumber, BigDecimal totalAmount) {
        String title = "Order Placed Successfully";
        String body = "Your order #" + orderNumber + " for $" + totalAmount + " has been confirmed.";

        logNotification(customerId, NotificationType.ORDER_PLACED, NotificationChannel.PUSH, title, body);
        pushNotificationService.sendPushNotification(customerId, title, body, Map.of("orderNumber", orderNumber));

        if (customerEmail != null && !customerEmail.isBlank()) {
            logNotification(customerId, NotificationType.ORDER_PLACED, NotificationChannel.EMAIL, title, body);
            emailNotificationService.sendOrderConfirmationEmail(customerEmail, orderNumber, totalAmount);
        }
    }

    @Transactional
    public void dispatchOutForDelivery(UUID customerId, String customerEmail, String shipmentNumber, String otp) {
        String title = "Shipment Out for Delivery";
        String body = "Shipment #" + shipmentNumber + " is out for delivery. Share OTP: " + otp + " with agent.";

        logNotification(customerId, NotificationType.OUT_FOR_DELIVERY, NotificationChannel.PUSH, title, body);
        pushNotificationService.sendPushNotification(customerId, title, body, Map.of("shipmentNumber", shipmentNumber, "otp", otp));

        if (customerEmail != null && !customerEmail.isBlank()) {
            logNotification(customerId, NotificationType.OUT_FOR_DELIVERY, NotificationChannel.EMAIL, title, body);
            emailNotificationService.sendDeliveryOtpEmail(customerEmail, shipmentNumber, otp);
        }
    }

    @Transactional
    public void dispatchRefundCompleted(UUID customerId, String customerEmail, UUID returnId, BigDecimal refundAmount) {
        String title = "Refund Processed";
        String body = "Your return #" + returnId + " has been inspected. Refund of $" + refundAmount + " is issued.";

        logNotification(customerId, NotificationType.RETURN_UPDATE, NotificationChannel.PUSH, title, body);
        pushNotificationService.sendPushNotification(customerId, title, body, Map.of("returnId", returnId.toString()));

        if (customerEmail != null && !customerEmail.isBlank()) {
            logNotification(customerId, NotificationType.RETURN_UPDATE, NotificationChannel.EMAIL, title, body);
            emailNotificationService.sendRefundConfirmationEmail(customerEmail, returnId, refundAmount);
        }
    }

    @Transactional
    public void dispatchSupportReply(UUID customerId, String ticketNumber, String messagePreview) {
        String title = "Support Ticket Update";
        String body = "New response on ticket #" + ticketNumber + ": " + messagePreview;

        logNotification(customerId, NotificationType.SUPPORT_REPLY, NotificationChannel.PUSH, title, body);
        pushNotificationService.sendPushNotification(customerId, title, body, Map.of("ticketNumber", ticketNumber));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID customerId, Pageable pageable) {
        return notificationLogRepository.findByUserIdOrderBySentAtDesc(customerId, pageable)
                .map(this::mapToResponse);
    }

    private void logNotification(UUID userId, NotificationType type, NotificationChannel channel, String title, String body) {
        NotificationLog notificationLog = NotificationLog.builder()
                .userId(userId)
                .type(type)
                .channel(channel)
                .title(title)
                .body(body)
                .status(NotificationStatus.SENT)
                .sentAt(Instant.now())
                .build();
        notificationLogRepository.save(notificationLog);
    }

    private NotificationResponse mapToResponse(NotificationLog log) {
        return NotificationResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .type(log.getType())
                .channel(log.getChannel())
                .title(log.getTitle())
                .body(log.getBody())
                .status(log.getStatus())
                .sentAt(log.getSentAt())
                .build();
    }
}
