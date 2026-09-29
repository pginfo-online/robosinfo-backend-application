package com.ecommerce.marketplace.support.dto;

import com.ecommerce.marketplace.support.model.SenderRole;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessageResponse {
    private UUID id;
    private UUID senderId;
    private SenderRole senderRole;
    private String message;
    private String attachmentUrls;
    private Boolean isInternalNote;
    private Instant createdAt;
}
