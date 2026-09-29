package com.ecommerce.marketplace.identity.dto;

import com.ecommerce.marketplace.identity.model.UserStatus;
import com.ecommerce.marketplace.identity.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;
    private String phone;
    private String email;
    private String name;
    private UserType userType;
    private UserStatus status;
    private Set<String> roles;
}
