package com.ecommerce.marketplace.support.service;

import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.support.dto.*;
import com.ecommerce.marketplace.support.model.*;
import com.ecommerce.marketplace.support.repository.SupportTicketRepository;
import com.ecommerce.marketplace.support.repository.TicketMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SupportTicketResponse createTicket(UUID customerId, CreateTicketRequest request) {
        String ticketNum = generateTicketNumber();
        Instant slaDue = Instant.now().plus(Duration.ofHours(24)); // 24-hour default SLA

        SupportTicket ticket = SupportTicket.builder()
            .ticketNumber(ticketNum)
            .customerId(customerId)
            .orderId(request.getOrderId())
            .orderItemId(request.getOrderItemId())
            .category(request.getCategory())
            .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
            .status(TicketStatus.OPEN)
            .subject(request.getSubject())
            .description(request.getDescription())
            .slaDueAt(slaDue)
            .isEscalated(false)
            .build();

        ticket = ticketRepository.save(ticket);

        // Add initial message
        TicketMessage initialMsg = TicketMessage.builder()
            .ticket(ticket)
            .senderId(customerId)
            .senderRole(SenderRole.CUSTOMER)
            .message(request.getDescription())
            .isInternalNote(false)
            .build();
        messageRepository.save(initialMsg);

        log.info("Created support ticket {} for customer {}", ticketNum, customerId);
        return toResponse(ticket, false);
    }

    @Transactional
    public TicketMessageResponse addMessage(UUID senderId, SenderRole senderRole, UUID ticketId, AddTicketMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        boolean isInternal = Boolean.TRUE.equals(request.getIsInternalNote());
        if (senderRole == SenderRole.CUSTOMER) {
            if (!ticket.getCustomerId().equals(senderId)) {
                throw new BusinessRuleException("Ticket does not belong to customer", "FORBIDDEN");
            }
            isInternal = false; // Customers cannot create internal notes
        }

        String attachmentJson = null;
        if (request.getAttachmentUrls() != null && !request.getAttachmentUrls().isEmpty()) {
            try {
                attachmentJson = objectMapper.writeValueAsString(request.getAttachmentUrls());
            } catch (Exception e) {
                attachmentJson = "[]";
            }
        }

        TicketMessage msg = TicketMessage.builder()
            .ticket(ticket)
            .senderId(senderId)
            .senderRole(senderRole)
            .message(request.getMessage())
            .attachmentUrls(attachmentJson)
            .isInternalNote(isInternal)
            .build();

        msg = messageRepository.save(msg);

        // Advance status if waiting on customer
        if (senderRole == SenderRole.CUSTOMER && ticket.getStatus() == TicketStatus.WAITING_ON_CUSTOMER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        return toMessageResponse(msg);
    }

    @Transactional
    public SupportTicketResponse updateTicketStatus(UUID agentId, UUID ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        ticket.setStatus(request.getStatus());
        if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
        }

        ticket = ticketRepository.save(ticket);

        if (request.getComment() != null && !request.getComment().isBlank()) {
            TicketMessage statusMsg = TicketMessage.builder()
                .ticket(ticket)
                .senderId(agentId)
                .senderRole(SenderRole.SUPPORT_AGENT)
                .message("Status changed to " + request.getStatus() + ": " + request.getComment())
                .isInternalNote(false)
                .build();
            messageRepository.save(statusMsg);
        }

        log.info("Ticket {} status updated to {}", ticket.getTicketNumber(), request.getStatus());
        return toResponse(ticket, true);
    }

    @Transactional
    public SupportTicketResponse assignTicket(UUID ticketId, UUID agentId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        ticket.setAssignedAgentId(agentId);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticket = ticketRepository.save(ticket);
        log.info("Assigned ticket {} to agent {}", ticket.getTicketNumber(), agentId);
        return toResponse(ticket, true);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketById(UUID userId, boolean isStaff, UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Support Ticket", ticketId.toString()));

        if (!isStaff && !ticket.getCustomerId().equals(userId)) {
            throw new BusinessRuleException("Ticket does not belong to customer", "FORBIDDEN");
        }

        return toResponse(ticket, isStaff);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> getCustomerTickets(UUID customerId, Pageable pageable) {
        Page<SupportTicket> page = ticketRepository.findByCustomerId(customerId, pageable);
        List<SupportTicketResponse> data = page.getContent().stream()
            .map(t -> toResponse(t, false))
            .collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> getAllTickets(TicketStatus status, Pageable pageable) {
        Page<SupportTicket> page = status != null
            ? ticketRepository.findByStatus(status, pageable)
            : ticketRepository.findAll(pageable);

        List<SupportTicketResponse> data = page.getContent().stream()
            .map(t -> toResponse(t, true))
            .collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    public SupportTicketResponse toResponse(SupportTicket t, boolean includeInternalNotes) {
        List<TicketMessage> msgs = includeInternalNotes
            ? messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId())
            : messageRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(t.getId());

        List<TicketMessageResponse> msgResponses = msgs.stream()
            .map(this::toMessageResponse)
            .collect(Collectors.toList());

        return SupportTicketResponse.builder()
            .id(t.getId())
            .ticketNumber(t.getTicketNumber())
            .customerId(t.getCustomerId())
            .orderId(t.getOrderId())
            .orderItemId(t.getOrderItemId())
            .category(t.getCategory())
            .priority(t.getPriority())
            .status(t.getStatus())
            .subject(t.getSubject())
            .description(t.getDescription())
            .assignedAgentId(t.getAssignedAgentId())
            .slaDueAt(t.getSlaDueAt())
            .isEscalated(t.getIsEscalated())
            .resolvedAt(t.getResolvedAt())
            .messages(msgResponses)
            .createdAt(t.getCreatedAt())
            .updatedAt(t.getUpdatedAt())
            .build();
    }

    public TicketMessageResponse toMessageResponse(TicketMessage m) {
        return TicketMessageResponse.builder()
            .id(m.getId())
            .senderId(m.getSenderId())
            .senderRole(m.getSenderRole())
            .message(m.getMessage())
            .attachmentUrls(m.getAttachmentUrls())
            .isInternalNote(m.getIsInternalNote())
            .createdAt(m.getCreatedAt())
            .build();
    }

    private String generateTicketNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "TCK-" + dateStr + "-" + rand;
    }
}
