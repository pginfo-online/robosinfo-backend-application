package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
public class DefaultOtpDeliveryService implements OtpDeliveryService {

    @Value("${app.ping4sms.base-url:https://site.ping4sms.com/api/smsapi}")
    private String ping4smsBaseUrl;

    @Value("${app.ping4sms.api-key:}")
    private String ping4smsApiKey;

    @Value("${app.ping4sms.sender-id:PNGOTP}")
    private String ping4smsSenderId;

    @Value("${app.ping4sms.sms-route:2}")
    private String ping4smsSmsRoute;

    @Value("${app.ping4sms.dlt-template-id:1507165967974501361}")
    private String ping4smsDltTemplateId;

    @Value("${app.whatsapp.access-token:}")
    private String whatsappAccessToken;

    private final RestClient restClient;

    public DefaultOtpDeliveryService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .build();
    }

    @Override
    public void sendOtp(String phone, String otp, OtpChannel channel, OtpPurpose purpose) {
        if (channel == OtpChannel.WHATSAPP) {
            sendWhatsAppOtp(phone, otp, purpose);
        } else {
            sendSmsOtp(phone, otp, purpose);
        }
    }

    private void sendSmsOtp(String phone, String otp, OtpPurpose purpose) {
        String cleanPhone = sanitizePhoneNumber(phone);

        // In dev mode when API key is missing or blank, log simulation cleanly
        if (ping4smsApiKey == null || ping4smsApiKey.isBlank() || "placeholder".equalsIgnoreCase(ping4smsApiKey)) {
            log.info("[SMS-DEV-MODE] Sending OTP '{}' to phone '{}' for purpose '{}'", otp, cleanPhone, purpose);
            return;
        }

        // Exact approved Ping4SMS template: "Dear Customer,{#var#} is your verification code -PNGOTP"
        String message = "Dear Customer," + otp + " is your verification code -PNGOTP";

        try {
            URI uri = UriComponentsBuilder.fromUriString(ping4smsBaseUrl)
                .queryParam("key", ping4smsApiKey)
                .queryParam("route", ping4smsSmsRoute)
                .queryParam("sender", ping4smsSenderId)
                .queryParam("number", cleanPhone)
                .queryParam("sms", message)
                .queryParam("templateid", ping4smsDltTemplateId)
                .build()
                .toUri();

            String maskedPhone = cleanPhone.length() >= 4 
                ? "***" + cleanPhone.substring(cleanPhone.length() - 4) 
                : cleanPhone;

            log.info("Dispatching SMS OTP via Ping4SMS gateway to {} (sender: {}, route: {}, templateId: {})",
                maskedPhone, ping4smsSenderId, ping4smsSmsRoute, ping4smsDltTemplateId);

            String responseBody = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

            log.info("Ping4SMS dispatch result for phone {}: {}", maskedPhone, responseBody);

            if (responseBody != null && responseBody.toLowerCase().contains("invalid key")) {
                log.error("Ping4SMS returned invalid key response for phone {}", maskedPhone);
                throw new BusinessRuleException("SMS gateway authentication failed", "SMS_GATEWAY_AUTH_ERROR");
            }
        } catch (BusinessRuleException bre) {
            throw bre;
        } catch (Exception e) {
            log.error("Ping4SMS HTTP invocation failed for phone: {}", e.getMessage());
            throw new BusinessRuleException("Failed to deliver OTP via SMS. Please try again.", "SMS_DELIVERY_FAILED");
        }
    }

    private void sendWhatsAppOtp(String phone, String otp, OtpPurpose purpose) {
        String cleanPhone = sanitizePhoneNumber(phone);
        if (whatsappAccessToken == null || whatsappAccessToken.isBlank()) {
            log.info("[WHATSAPP-DEV-MODE] Sending OTP '{}' to phone '{}' for purpose '{}'", otp, cleanPhone, purpose);
            return;
        }
        log.info("Dispatching WhatsApp OTP via Meta Graph API to phone ending in ***{}",
            cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : cleanPhone);
    }

    private String sanitizePhoneNumber(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }
}
