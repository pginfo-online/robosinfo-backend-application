package com.ecommerce.marketplace.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectReasonRequest {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
