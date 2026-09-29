package com.ecommerce.marketplace.notification.service;

import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;

public interface OtpDeliveryService {

    void sendOtp(String phone, String otp, OtpChannel channel, OtpPurpose purpose);
}
