package com.ecommerce.marketplace.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private AdminUserDto admin;
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private boolean requiresMfa = false;
    private String tempToken;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserDto {
        private UUID id;
        private String email;
        private String fullName;
        private String role;
        private boolean mfaEnabled;
    }
}
