package com.ecommerce.marketplace.identity.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.identity.dto.AuthResponse;
import com.ecommerce.marketplace.identity.dto.SendOtpRequest;
import com.ecommerce.marketplace.identity.dto.VerifyOtpRequest;
import com.ecommerce.marketplace.identity.model.OtpChannel;
import com.ecommerce.marketplace.identity.model.OtpPurpose;
import com.ecommerce.marketplace.identity.model.OtpRequest;
import com.ecommerce.marketplace.identity.model.User;
import com.ecommerce.marketplace.identity.model.UserStatus;
import com.ecommerce.marketplace.identity.model.UserType;
import com.ecommerce.marketplace.identity.repository.OtpRequestRepository;
import com.ecommerce.marketplace.identity.repository.RefreshTokenRepository;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.identity.security.JwtTokenProvider;
import com.ecommerce.marketplace.notification.service.OtpDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthOtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRequestRepository otpRequestRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpDeliveryService otpDeliveryService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 3);
        ReflectionTestUtils.setField(authService, "maxOtpRequestsPerHour", 5);
        ReflectionTestUtils.setField(authService, "resendCooldownSeconds", 30);
        ReflectionTestUtils.setField(authService, "testPhone", "9999999999");
        ReflectionTestUtils.setField(authService, "testOtp", "123456");
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should successfully send OTP and trigger delivery service")
    void testSendOtpSuccess() {
        SendOtpRequest request = new SendOtpRequest("9876543210", OtpChannel.SMS, OtpPurpose.LOGIN);
        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(eq("9876543210"), eq(OtpPurpose.LOGIN)))
            .thenReturn(Optional.empty());
        when(otpRequestRepository.countByPhoneAndCreatedAtAfter(eq("9876543210"), any(Instant.class)))
            .thenReturn(0L);

        authService.sendOtp(request);

        ArgumentCaptor<OtpRequest> captor = ArgumentCaptor.forClass(OtpRequest.class);
        verify(otpRequestRepository).save(captor.capture());
        OtpRequest saved = captor.getValue();
        assertEquals("9876543210", saved.getPhone());
        assertEquals(OtpChannel.SMS, saved.getChannel());
        assertEquals(OtpPurpose.LOGIN, saved.getPurpose());
        assertNotNull(saved.getOtpHash());

        verify(otpDeliveryService).sendOtp(eq("9876543210"), anyString(), eq(OtpChannel.SMS), eq(OtpPurpose.LOGIN));
    }

    @Test
    @DisplayName("Should reject send OTP when in cooldown window")
    void testSendOtpCooldown() {
        SendOtpRequest request = new SendOtpRequest("9876543210", OtpChannel.SMS, OtpPurpose.LOGIN);
        OtpRequest recentOtp = OtpRequest.builder()
            .phone("9876543210")
            .createdAt(Instant.now().minus(Duration.ofSeconds(10)))
            .build();

        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(eq("9876543210"), eq(OtpPurpose.LOGIN)))
            .thenReturn(Optional.of(recentOtp));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> authService.sendOtp(request));
        assertEquals("OTP_COOLDOWN", ex.getErrorCode());
        verify(otpDeliveryService, never()).sendOtp(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject send OTP when hourly rate limit is exceeded")
    void testSendOtpHourlyRateLimit() {
        SendOtpRequest request = new SendOtpRequest("9876543210", OtpChannel.SMS, OtpPurpose.LOGIN);
        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(eq("9876543210"), eq(OtpPurpose.LOGIN)))
            .thenReturn(Optional.empty());
        when(otpRequestRepository.countByPhoneAndCreatedAtAfter(eq("9876543210"), any(Instant.class)))
            .thenReturn(5L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> authService.sendOtp(request));
        assertEquals("RATE_LIMIT_EXCEEDED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should bypass external SMS for reviewer test phone")
    void testSendOtpReviewerPhone() {
        SendOtpRequest request = new SendOtpRequest("9999999999", OtpChannel.SMS, OtpPurpose.LOGIN);
        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(eq("9999999999"), eq(OtpPurpose.LOGIN)))
            .thenReturn(Optional.empty());
        when(otpRequestRepository.countByPhoneAndCreatedAtAfter(eq("9999999999"), any(Instant.class)))
            .thenReturn(0L);

        authService.sendOtp(request);

        ArgumentCaptor<OtpRequest> captor = ArgumentCaptor.forClass(OtpRequest.class);
        verify(otpRequestRepository).save(captor.capture());
        OtpRequest saved = captor.getValue();
        assertEquals(hashString("123456"), saved.getOtpHash());

        // External SMS service should NOT be called for test account
        verify(otpDeliveryService, never()).sendOtp(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should verify valid OTP and authenticate user")
    void testVerifyOtpSuccess() {
        String phone = "9876543210";
        String otp = "654321";
        VerifyOtpRequest request = new VerifyOtpRequest(phone, otp, OtpPurpose.LOGIN);

        OtpRequest storedOtp = OtpRequest.builder()
            .phone(phone)
            .otpHash(hashString(otp))
            .purpose(OtpPurpose.LOGIN)
            .attempts(0)
            .maxAttempts(3)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
            .build();

        User user = User.builder()
            .id(UUID.randomUUID())
            .phone(phone)
            .status(UserStatus.ACTIVE)
            .userType(UserType.CUSTOMER)
            .build();

        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN))
            .thenReturn(Optional.of(storedOtp));
        when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(tokenProvider.getRefreshTokenExpiryMs()).thenReturn(604800000L);

        AuthResponse response = authService.verifyOtp(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertNotNull(storedOtp.getVerifiedAt());
        verify(otpRequestRepository, times(1)).save(storedOtp);
    }

    @Test
    @DisplayName("Should increment attempts and reject incorrect OTP")
    void testVerifyOtpIncorrect() {
        String phone = "9876543210";
        VerifyOtpRequest request = new VerifyOtpRequest(phone, "000000", OtpPurpose.LOGIN);

        OtpRequest storedOtp = OtpRequest.builder()
            .phone(phone)
            .otpHash(hashString("654321"))
            .purpose(OtpPurpose.LOGIN)
            .attempts(0)
            .maxAttempts(3)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
            .build();

        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN))
            .thenReturn(Optional.of(storedOtp));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> authService.verifyOtp(request));
        assertEquals("INVALID_OTP", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("2 attempt(s) remaining"));
        assertEquals(1, storedOtp.getAttempts());
        verify(otpRequestRepository).save(storedOtp);
    }

    @Test
    @DisplayName("Should reject expired OTP")
    void testVerifyOtpExpired() {
        String phone = "9876543210";
        VerifyOtpRequest request = new VerifyOtpRequest(phone, "654321", OtpPurpose.LOGIN);

        OtpRequest storedOtp = OtpRequest.builder()
            .phone(phone)
            .otpHash(hashString("654321"))
            .purpose(OtpPurpose.LOGIN)
            .attempts(0)
            .maxAttempts(3)
            .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
            .build();

        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN))
            .thenReturn(Optional.of(storedOtp));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> authService.verifyOtp(request));
        assertEquals("OTP_EXPIRED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Should reject already verified OTP")
    void testVerifyOtpAlreadyUsed() {
        String phone = "9876543210";
        VerifyOtpRequest request = new VerifyOtpRequest(phone, "654321", OtpPurpose.LOGIN);

        OtpRequest storedOtp = OtpRequest.builder()
            .phone(phone)
            .otpHash(hashString("654321"))
            .purpose(OtpPurpose.LOGIN)
            .attempts(1)
            .maxAttempts(3)
            .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
            .verifiedAt(Instant.now().minus(Duration.ofMinutes(2)))
            .build();

        when(otpRequestRepository.findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, OtpPurpose.LOGIN))
            .thenReturn(Optional.of(storedOtp));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> authService.verifyOtp(request));
        assertEquals("OTP_ALREADY_USED", ex.getErrorCode());
    }
}
