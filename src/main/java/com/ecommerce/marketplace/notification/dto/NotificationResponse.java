package com.ecommerce.marketplace.notification.dto;

import com.ecommerce.marketplace.notification.model.NotificationChannel;
import com.ecommerce.marketplace.notification.model.NotificationStatus;
import com.ecommerce.marketplace.notification.model.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private NotificationChannel channel;
    private String title;
    private String body;
    private NotificationStatus status;
    private Instant sentAt;
}
