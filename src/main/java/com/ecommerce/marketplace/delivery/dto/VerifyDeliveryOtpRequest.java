package com.ecommerce.marketplace.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyDeliveryOtpRequest {

    @NotBlank(message = "Delivery OTP is required")
    @Size(min = 4, max = 10, message = "OTP must be between 4 and 10 characters")
    private String otp;
}
