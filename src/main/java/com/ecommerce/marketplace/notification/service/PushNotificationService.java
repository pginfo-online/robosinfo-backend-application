package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.notification.dto.RegisterDeviceTokenRequest;
import com.ecommerce.marketplace.notification.model.DevicePlatform;
import com.ecommerce.marketplace.notification.model.DeviceToken;
import com.ecommerce.marketplace.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void registerDeviceToken(UUID userId, RegisterDeviceTokenRequest request) {
        log.info("Registering device token for userId={} platform={}", userId, request.getPlatform());
        DeviceToken token = deviceTokenRepository.findByUserIdAndToken(userId, request.getToken())
                .orElseGet(() -> DeviceToken.builder()
                        .userId(userId)
                        .token(request.getToken())
                        .platform(request.getPlatform() != null ? request.getPlatform() : DevicePlatform.ANDROID)
                        .build());

        token.setPlatform(request.getPlatform() != null ? request.getPlatform() : DevicePlatform.ANDROID);
        token.setUpdatedAt(Instant.now());
        deviceTokenRepository.save(token);
    }

    @Transactional
    public void unregisterDeviceToken(String token) {
        log.info("Unregistering device token");
        deviceTokenRepository.deleteByToken(token);
    }

    public void sendPushNotification(UUID userId, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.info("No active device tokens found for userId={}. Skipping push delivery.", userId);
            return;
        }

        for (DeviceToken token : tokens) {
            sendToDevice(token.getToken(), token.getPlatform(), title, body, data);
        }
    }

    private void sendToDevice(String token, DevicePlatform platform, String title, String body, Map<String, String> data) {
        // FCM / APNS integration point:
        // In local/production without Firebase service account, logs formatted payload
        log.info("[PUSH-DISPATCH] platform={} targetToken={} title='{}' body='{}' data={}",
                platform, token.length() > 10 ? token.substring(0, 10) + "..." : token, title, body, data);
    }
}
