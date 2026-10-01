package com.ecommerce.marketplace.identity.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.common.exception.UnauthorizedException;
import com.ecommerce.marketplace.identity.dto.*;
import com.ecommerce.marketplace.identity.model.*;
import com.ecommerce.marketplace.identity.repository.OtpRequestRepository;
import com.ecommerce.marketplace.identity.repository.RefreshTokenRepository;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.identity.security.JwtTokenProvider;
import com.ecommerce.marketplace.notification.service.OtpDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRequestRepository otpRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int otpMaxAttempts;

    @Value("${app.otp.max-requests-per-hour:5}")
    private int maxOtpRequestsPerHour;

    @Value("${app.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds;

    @Value("${app.otp.test-phone:9999999999}")
    private String testPhone;

    @Value("${app.otp.test-otp:123456}")
    private String testOtp;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String cleanPhone = sanitizePhone(request.getPhone());
        if (cleanPhone.length() != 10) {
            throw new BusinessRuleException("Phone must be a valid 10-digit Indian mobile number", "INVALID_PHONE");
        }
        request.setPhone(cleanPhone);

        // 1. Resend Cooldown Check (e.g. 30 seconds between successive requests)
        otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(cleanPhone, request.getPurpose())
            .ifPresent(lastOtp -> {
                if (lastOtp.getCreatedAt() != null) {
                    long elapsedSeconds = Duration.between(lastOtp.getCreatedAt(), Instant.now()).getSeconds();
                    if (elapsedSeconds < resendCooldownSeconds) {
                        long remaining = resendCooldownSeconds - elapsedSeconds;
                        throw new BusinessRuleException(
                            "Please wait " + remaining + " seconds before requesting a new OTP.",
                            "OTP_COOLDOWN"
                        );
                    }
                }
            });

        // 2. Hourly Rate Limiting Check
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        long recentRequests = otpRequestRepository.countByPhoneAndCreatedAtAfter(cleanPhone, oneHourAgo);
        if (recentRequests >= maxOtpRequestsPerHour) {
            throw new BusinessRuleException("Too many OTP requests. Please try again later.", "RATE_LIMIT_EXCEEDED");
        }

        // 3. Reviewer / Test Phone Support
        String otp;
        boolean isTestPhone = cleanPhone.equals(testPhone);
        if (isTestPhone) {
            otp = testOtp;
            log.info("[TEST-ACCOUNT] Using test OTP for reviewer phone {}", cleanPhone);
        } else {
            otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        }

        String otpHash = hashString(otp);

        OtpRequest otpRequest = OtpRequest.builder()
            .phone(cleanPhone)
            .otpHash(otpHash)
            .channel(request.getChannel())
            .purpose(request.getPurpose())
            .maxAttempts(otpMaxAttempts)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(otpExpiryMinutes)))
            .build();

        otpRequestRepository.save(otpRequest);

        // 4. Dispatch via SMS / WhatsApp if not test account
        if (!isTestPhone) {
            otpDeliveryService.sendOtp(cleanPhone, otp, request.getChannel(), request.getPurpose());
        }
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String cleanPhone = sanitizePhone(request.getPhone());
        request.setPhone(cleanPhone);

        OtpRequest otpRequest = otpRequestRepository
            .findTopByPhoneAndPurposeOrderByCreatedAtDesc(cleanPhone, request.getPurpose())
            .orElseThrow(() -> new BusinessRuleException("No active OTP request found for this phone", "INVALID_OTP"));

        if (otpRequest.isVerified()) {
            throw new BusinessRuleException("This OTP has already been verified. Please request a new one.", "OTP_ALREADY_USED");
        }

        if (otpRequest.isExpired()) {
            throw new BusinessRuleException("OTP has expired. Please request a new verification code.", "OTP_EXPIRED");
        }

        if (otpRequest.getAttempts() >= otpRequest.getMaxAttempts()) {
            throw new BusinessRuleException("Maximum verification attempts exceeded. Please request a new OTP.", "MAX_ATTEMPTS_EXCEEDED");
        }

        otpRequest.setAttempts(otpRequest.getAttempts() + 1);

        String inputOtp = request.getOtp() != null ? request.getOtp().trim() : "";
        String inputHash = hashString(inputOtp);
        if (!inputHash.equals(otpRequest.getOtpHash())) {
            otpRequestRepository.save(otpRequest);
            int remaining = otpRequest.getMaxAttempts() - otpRequest.getAttempts();
            if (remaining > 0) {
                throw new BusinessRuleException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.",
                    "INVALID_OTP"
                );
            } else {
                throw new BusinessRuleException(
                    "Incorrect OTP. Maximum attempts reached. Please request a new code.",
                    "MAX_ATTEMPTS_EXCEEDED"
                );
            }
        }

        otpRequest.setVerifiedAt(Instant.now());
        otpRequestRepository.save(otpRequest);

        // Find or create customer
        User user = userRepository.findByPhone(cleanPhone)
            .orElseGet(() -> {
                User newUser = User.builder()
                    .phone(cleanPhone)
                    .userType(UserType.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .build();
                newUser.addRole(RoleName.ROLE_CUSTOMER);
                return userRepository.save(newUser);
            });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("ACCOUNT_SUSPENDED", "Your account is not active");
        }

        return generateAuthResponse(user);
    }

    private String sanitizePhone(String rawPhone) {
        if (rawPhone == null) return "";
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessRuleException("Phone number is already registered", "PHONE_ALREADY_EXISTS");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email address is already registered", "EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
            .phone(request.getPhone())
            .email(request.getEmail())
            .name(request.getName())
            .userType(request.getUserType())
            .status(UserStatus.ACTIVE)
            .passwordHash(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
            .build();

        if (request.getUserType() == UserType.SELLER) {
            user.addRole(RoleName.ROLE_SELLER);
        } else if (request.getUserType() == UserType.DELIVERY) {
            user.addRole(RoleName.ROLE_DELIVERY_PARTNER);
        } else {
            user.addRole(RoleName.ROLE_CUSTOMER);
        }

        user = userRepository.save(user);
        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getLoginIdentifier();
        if (identifier.isBlank()) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Email or phone number is required");
        }

        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));
        } else {
            user = userRepository.findByPhone(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid phone/email or password"));
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("ACCOUNT_SUSPENDED", "Your account is not active");
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String tokenHash = hashString(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (storedToken.getRevoked()) {
            // Potential token reuse attack - revoke all in token family!
            refreshTokenRepository.revokeAllByTokenFamily(storedToken.getTokenFamily());
            throw new UnauthorizedException("TOKEN_REUSE_DETECTED", "Refresh token was already used or revoked");
        }

        if (storedToken.isExpired()) {
            throw new UnauthorizedException("TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Revoke the old token
        storedToken.setRevoked(true);
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User no longer exists"));

        // Issue new tokens keeping the same token family
        return generateAuthResponseWithFamily(user, storedToken.getTokenFamily());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = hashString(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        return generateAuthResponseWithFamily(user, UUID.randomUUID());
    }

    private AuthResponse generateAuthResponseWithFamily(User user, UUID tokenFamily) {
        Set<String> roles = user.getRoles().stream()
            .map(UserRole::getRole)
            .collect(Collectors.toSet());

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getPhone(), roles);

        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(hashString(rawRefreshToken))
            .tokenFamily(tokenFamily)
            .expiresAt(Instant.now().plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpiryMs())))
            .build();

        refreshTokenRepository.save(refreshTokenEntity);

        UserProfileResponse profile = UserProfileResponse.builder()
            .id(user.getId())
            .phone(user.getPhone())
            .email(user.getEmail())
            .name(user.getName())
            .userType(user.getUserType())
            .status(user.getStatus())
            .roles(roles)
            .build();

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(rawRefreshToken)
            .expiresInMs(900_000)
            .user(profile)
            .build();
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
