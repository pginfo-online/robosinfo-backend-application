package com.ecommerce.marketplace.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    public void sendOrderConfirmationEmail(String recipientEmail, String orderNumber, BigDecimal totalAmount) {
        String subject = "Order Confirmation - " + orderNumber;
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Thank you for your order!</h2>
                    <p>Your order <strong>#%s</strong> has been placed successfully.</p>
                    <p>Total Paid: <strong>$%s</strong></p>
                    <p>We are preparing your shipment and will notify you when it is dispatched.</p>
                </body>
                </html>
                """, orderNumber, totalAmount);

        sendEmail(recipientEmail, subject, htmlBody);
    }

    public void sendDeliveryOtpEmail(String recipientEmail, String shipmentNumber, String otp) {
        String subject = "Your Delivery OTP for Shipment - " + shipmentNumber;
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Shipment Out for Delivery</h2>
                    <p>Your shipment <strong>#%s</strong> is out for delivery today.</p>
                    <p style="font-size: 24px; font-weight: bold; color: #16a34a; letter-spacing: 4px;">OTP: %s</p>
                    <p>Please share this 4-digit code with your delivery agent upon package arrival.</p>
                </body>
                </html>
                """, shipmentNumber, otp);

        sendEmail(recipientEmail, subject, htmlBody);
    }

    public void sendRefundConfirmationEmail(String recipientEmail, UUID returnId, BigDecimal refundAmount) {
        String subject = "Refund Processed for Return #" + returnId;
        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>Refund Completed</h2>
                    <p>Your return request has been inspected and approved.</p>
                    <p>Refund Amount Credited: <strong>$%s</strong></p>
                    <p>Funds will reflect back to your original payment method in 3-5 business days.</p>
                </body>
                </html>
                """, refundAmount);

        sendEmail(recipientEmail, subject, htmlBody);
    }

    public void sendEmail(String toEmail, String subject, String body) {
        // SMTP / SendGrid / Amazon SES integration point:
        // Logs the rendered HTML email receipt for audit & verification
        log.info("[EMAIL-DISPATCH] to='{}' subject='{}' bodyLength={}", toEmail, subject, body.length());
    }
}
