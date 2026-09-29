package com.ecommerce.marketplace.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String phone;

    private String email;

    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    public String getLoginIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return phone != null ? phone.trim() : "";
    }
}
