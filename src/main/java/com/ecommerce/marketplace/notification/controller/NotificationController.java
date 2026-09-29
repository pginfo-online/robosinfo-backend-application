package com.ecommerce.marketplace.notification.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.notification.dto.NotificationResponse;
import com.ecommerce.marketplace.notification.dto.RegisterDeviceTokenRequest;
import com.ecommerce.marketplace.notification.service.NotificationHub;
import com.ecommerce.marketplace.notification.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications & Messaging", description = "Endpoints for device push token registration and notification feed")
public class NotificationController {

    private final PushNotificationService pushNotificationService;
    private final NotificationHub notificationHub;

    @PostMapping("/device-token")
    @Operation(summary = "Register FCM or APNS device push token for push notifications")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        pushNotificationService.registerDeviceToken(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Device token registered successfully", null));
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Unregister device token on logout or app uninstall")
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(@RequestParam String token) {
        pushNotificationService.unregisterDeviceToken(token);
        return ResponseEntity.ok(ApiResponse.success("Device token unregistered successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get notification history for authenticated user")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<NotificationResponse> response = PageResponse.from(notificationHub.getMyNotifications(principal.getId(), pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
