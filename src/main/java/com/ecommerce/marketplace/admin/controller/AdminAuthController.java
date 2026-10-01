package com.ecommerce.marketplace.admin.controller;

import com.ecommerce.marketplace.admin.dto.AdminLoginRequest;
import com.ecommerce.marketplace.admin.dto.AdminLoginResponse;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.exception.UnauthorizedException;
import com.ecommerce.marketplace.identity.dto.AuthResponse;
import com.ecommerce.marketplace.identity.dto.LoginRequest;
import com.ecommerce.marketplace.identity.dto.TokenRefreshRequest;
import com.ecommerce.marketplace.identity.model.RoleName;
import com.ecommerce.marketplace.identity.model.User;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "Endpoints for Admin Control Plane authentication")
public class AdminAuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    @Operation(summary = "Authenticate platform administrator")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail().trim().toLowerCase());
        loginRequest.setPassword(request.getPassword());

        AuthResponse authResponse = authService.login(loginRequest);

        if (authResponse.getUser() == null || authResponse.getUser().getRoles() == null) {
            throw new UnauthorizedException("ACCESS_DENIED", "Access denied: Missing role permissions");
        }

        boolean hasAdminRole = authResponse.getUser().getRoles().stream()
            .anyMatch(r -> r.contains("ADMIN") || r.contains("SUPER_ADMIN"));

        if (!hasAdminRole) {
            throw new UnauthorizedException("ACCESS_DENIED", "Access denied: User account is not an administrator");
        }

        boolean isSuperAdmin = authResponse.getUser().getRoles().contains("ROLE_SUPER_ADMIN");
        String primaryRole = isSuperAdmin ? "SUPER_ADMIN" : "OPS_ADMIN";

        AdminLoginResponse.AdminUserDto adminDto = AdminLoginResponse.AdminUserDto.builder()
            .id(authResponse.getUser().getId())
            .email(authResponse.getUser().getEmail())
            .fullName(authResponse.getUser().getName() != null ? authResponse.getUser().getName() : "Super Administrator")
            .role(primaryRole)
            .mfaEnabled(false)
            .build();

        AdminLoginResponse response = AdminLoginResponse.builder()
            .admin(adminDto)
            .accessToken(authResponse.getAccessToken())
            .refreshToken(authResponse.getRefreshToken())
            .requiresMfa(false)
            .build();

        log.info("Admin successfully authenticated: {} with roles: {}", authResponse.getUser().getEmail(), authResponse.getUser().getRoles());
        return ResponseEntity.ok(ApiResponse.success("Admin authorized successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh admin session token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated admin user profile")
    public ResponseEntity<ApiResponse<AdminLoginResponse.AdminUserDto>> getCurrentAdmin(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Not authenticated");
        }

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new UnauthorizedException("USER_NOT_FOUND", "Admin profile not found"));

        boolean isSuperAdmin = user.getRoles().stream()
            .anyMatch(r -> r.getRole() == RoleName.ROLE_SUPER_ADMIN.name());
        String primaryRole = isSuperAdmin ? "SUPER_ADMIN" : "OPS_ADMIN";

        AdminLoginResponse.AdminUserDto adminDto = AdminLoginResponse.AdminUserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getName())
            .role(primaryRole)
            .mfaEnabled(false)
            .build();

        return ResponseEntity.ok(ApiResponse.success(adminDto));
    }
}
