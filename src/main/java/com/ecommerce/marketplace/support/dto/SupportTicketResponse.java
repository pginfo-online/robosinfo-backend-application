package com.ecommerce.marketplace.support.dto;

import com.ecommerce.marketplace.support.model.TicketCategory;
import com.ecommerce.marketplace.support.model.TicketPriority;
import com.ecommerce.marketplace.support.model.TicketStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponse {
    private UUID id;
    private String ticketNumber;
    private UUID customerId;
    private UUID orderId;
    private UUID orderItemId;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private String subject;
    private String description;
    private UUID assignedAgentId;
    private Instant slaDueAt;
    private Boolean isEscalated;
    private Instant resolvedAt;
    private List<TicketMessageResponse> messages;
    private Instant createdAt;
    private Instant updatedAt;
}
