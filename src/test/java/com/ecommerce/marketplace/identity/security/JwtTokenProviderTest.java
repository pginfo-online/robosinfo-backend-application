package com.ecommerce.marketplace.identity.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "test-secret-key-that-is-at-least-32-chars-long-for-hmac-sha256");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiryMs", 900000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiryMs", 604800000L);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse claims")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String phone = "9876543210";
        List<String> roles = List.of("ROLE_CUSTOMER", "ROLE_SELLER");

        String token = tokenProvider.generateAccessToken(userId, phone, roles);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));

        assertEquals(userId, tokenProvider.getUserId(token));
        assertEquals(phone, tokenProvider.getPhone(token));
        assertEquals(roles, tokenProvider.getRoles(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered token")
    void testInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.string"));
    }
}
