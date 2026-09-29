package com.ecommerce.marketplace.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddTicketMessageRequest {

    @NotBlank(message = "Message cannot be blank")
    private String message;

    private List<String> attachmentUrls;
    @Builder.Default
    private Boolean isInternalNote = false;
}
