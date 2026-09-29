package com.ecommerce.marketplace.support.dto;

import com.ecommerce.marketplace.support.model.TicketCategory;
import com.ecommerce.marketplace.support.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    private UUID orderId;
    private UUID orderItemId;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;
}
