package com.ecommerce.marketplace.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RazorpayConfig {

    @Value("${app.razorpay.key-id:rzp_test_placeholder}")
    private String keyId;

    @Value("${app.razorpay.key-secret:secret_placeholder}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            String actualKey = (keyId != null && !keyId.isBlank()) ? keyId : "rzp_test_placeholder";
            String actualSecret = (keySecret != null && !keySecret.isBlank()) ? keySecret : "secret_placeholder";
            return new RazorpayClient(actualKey, actualSecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize RazorpayClient: {}", e.getMessage());
            return null;
        }
    }
}
