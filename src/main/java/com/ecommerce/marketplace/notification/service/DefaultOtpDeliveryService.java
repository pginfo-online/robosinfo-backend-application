package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultOtpDeliveryService implements OtpDeliveryService {

    @Value("${app.ping4sms.api-key:}")
    private String ping4smsApiKey;

    @Value("${app.whatsapp.access-token:}")
    private String whatsappAccessToken;

    @Override
    public void sendOtp(String phone, String otp, OtpChannel channel, OtpPurpose purpose) {
        if (channel == OtpChannel.WHATSAPP) {
            sendWhatsAppOtp(phone, otp, purpose);
        } else {
            sendSmsOtp(phone, otp, purpose);
        }
    }

    private void sendSmsOtp(String phone, String otp, OtpPurpose purpose) {
        if (ping4smsApiKey == null || ping4smsApiKey.isBlank()) {
            log.info("[SMS-DEV-MODE] Sending OTP '{}' to phone '{}' for purpose '{}'", otp, phone, purpose);
            return;
        }
        log.info("Dispatching SMS OTP via Ping4SMS gateway to {}", phone);
        // Ping4SMS HTTP client invocation (placeholder for HTTP call)
    }

    private void sendWhatsAppOtp(String phone, String otp, OtpPurpose purpose) {
        if (whatsappAccessToken == null || whatsappAccessToken.isBlank()) {
            log.info("[WHATSAPP-DEV-MODE] Sending OTP '{}' to phone '{}' for purpose '{}'", otp, phone, purpose);
            return;
        }
        log.info("Dispatching WhatsApp OTP via Meta Graph API to {}", phone);
        // Meta Graph API invocation (placeholder for HTTP call)
    }
}
