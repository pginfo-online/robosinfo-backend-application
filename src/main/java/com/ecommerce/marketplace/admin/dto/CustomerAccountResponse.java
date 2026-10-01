package com.ecommerce.marketplace.admin.dto;

import com.ecommerce.marketplace.identity.model.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private Long totalOrders;
    private Long totalSpentPaisa;
    private Instant registeredAt;
}
