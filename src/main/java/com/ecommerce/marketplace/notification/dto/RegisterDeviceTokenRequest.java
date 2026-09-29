package com.ecommerce.marketplace.notification.dto;

import com.ecommerce.marketplace.notification.model.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    private String token;

    @Builder.Default
    private DevicePlatform platform = DevicePlatform.ANDROID;
}
