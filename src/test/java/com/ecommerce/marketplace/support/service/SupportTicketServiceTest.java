package com.ecommerce.marketplace.support.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.support.dto.*;
import com.ecommerce.marketplace.support.model.*;
import com.ecommerce.marketplace.support.repository.SupportTicketRepository;
import com.ecommerce.marketplace.support.repository.TicketMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private TicketMessageRepository messageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private UUID customerId;
    private UUID ticketId;
    private SupportTicket ticket;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        ticketId = UUID.randomUUID();

        ticket = SupportTicket.builder()
                .id(ticketId)
                .ticketNumber("TCK-TEST-999")
                .customerId(customerId)
                .category(TicketCategory.LATE_DELIVERY)
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.OPEN)
                .subject("Package missing")
                .description("My package was not delivered")
                .slaDueAt(Instant.now().plusSeconds(86400))
                .isEscalated(false)
                .messages(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create support ticket with initial customer message and 24h SLA")
    void testCreateTicket() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .category(TicketCategory.LATE_DELIVERY)
                .priority(TicketPriority.HIGH)
                .subject("Package missing")
                .description("My package was not delivered")
                .build();

        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> {
            SupportTicket t = i.getArgument(0);
            t.setId(ticketId);
            return t;
        });

        SupportTicketResponse response = supportTicketService.createTicket(customerId, request);

        assertNotNull(response);
        assertEquals(TicketStatus.OPEN, response.getStatus());
        assertEquals(TicketPriority.HIGH, response.getPriority());
        assertNotNull(response.getSlaDueAt());
        verify(ticketRepository).save(any(SupportTicket.class));
        verify(messageRepository).save(any(TicketMessage.class));
    }

    @Test
    @DisplayName("Should prevent customer from replying to another user's ticket")
    void testCustomerSecurityValidation() {
        UUID otherCustomer = UUID.randomUUID();
        AddTicketMessageRequest msgReq = AddTicketMessageRequest.builder()
                .message("Checking in")
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                supportTicketService.addMessage(otherCustomer, SenderRole.CUSTOMER, ticketId, msgReq)
        );

        assertTrue(ex.getMessage().contains("Ticket does not belong to customer"));
    }

    @Test
    @DisplayName("Should allow agent to update status to RESOLVED and set resolvedAt")
    void testResolveTicket() {
        UUID agentId = UUID.randomUUID();
        UpdateTicketStatusRequest req = UpdateTicketStatusRequest.builder()
                .status(TicketStatus.RESOLVED)
                .comment("Replacement dispatched")
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> i.getArgument(0));

        SupportTicketResponse response = supportTicketService.updateTicketStatus(agentId, ticketId, req);

        assertNotNull(response);
        assertEquals(TicketStatus.RESOLVED, response.getStatus());
        assertNotNull(ticket.getResolvedAt());
        verify(ticketRepository).save(ticket);
        verify(messageRepository).save(any(TicketMessage.class));
    }
}
