package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class DefaultOtpDeliveryServiceTest {

    private DefaultOtpDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new DefaultOtpDeliveryService();
        ReflectionTestUtils.setField(deliveryService, "ping4smsBaseUrl", "https://site.ping4sms.com/api/smsapi");
        ReflectionTestUtils.setField(deliveryService, "ping4smsSenderId", "PNGOTP");
        ReflectionTestUtils.setField(deliveryService, "ping4smsSmsRoute", "2");
        ReflectionTestUtils.setField(deliveryService, "ping4smsDltTemplateId", "1507165967974501361");
    }

    @Test
    @DisplayName("Should run in dev-mode cleanly when API key is blank or placeholder")
    void testDevModeWhenApiKeyBlank() {
        ReflectionTestUtils.setField(deliveryService, "ping4smsApiKey", "");

        // Should not throw any exception and gracefully log simulation
        assertDoesNotThrow(() -> 
            deliveryService.sendOtp("+91 98765 43210", "123456", OtpChannel.SMS, OtpPurpose.LOGIN)
        );
    }

    @Test
    @DisplayName("Should sanitize phone numbers with +91, 0, or formatting")
    void testSanitizePhoneNumber() {
        String res1 = ReflectionTestUtils.invokeMethod(deliveryService, "sanitizePhoneNumber", "+91 98765 43210");
        assertEquals("9876543210", res1);

        String res2 = ReflectionTestUtils.invokeMethod(deliveryService, "sanitizePhoneNumber", "09876543210");
        assertEquals("9876543210", res2);

        String res3 = ReflectionTestUtils.invokeMethod(deliveryService, "sanitizePhoneNumber", "9876543210");
        assertEquals("9876543210", res3);
    }
}
