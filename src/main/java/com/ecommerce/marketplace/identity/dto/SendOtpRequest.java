package com.ecommerce.marketplace.identity.dto;

import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
    private String phone;

    @NotNull(message = "Channel is required (SMS or WHATSAPP)")
    private OtpChannel channel = OtpChannel.SMS;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose = OtpPurpose.LOGIN;
}
