package com.ecommerce.marketplace.support.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.model.RoleName;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import com.ecommerce.marketplace.support.dto.*;
import com.ecommerce.marketplace.support.model.SenderRole;
import com.ecommerce.marketplace.support.model.TicketStatus;
import com.ecommerce.marketplace.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Customer Support & Helpdesk", description = "Endpoints for support ticket management, customer-agent message threads, and SLA status tracking")
public class SupportTicketController {

    private final SupportTicketService ticketService;

    @PostMapping
    @Operation(summary = "Create a customer support ticket linked to an order or inquiry")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request) {
        SupportTicketResponse response = ticketService.createTicket(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Ticket created", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get paginated tickets created by current authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SupportTicketResponse> response = ticketService.getCustomerTickets(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get support ticket conversation history and details by ID")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        boolean isStaff = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()) ||
                           a.getAuthority().equals(RoleName.ROLE_SUPER_ADMIN.name()) ||
                           a.getAuthority().equals(RoleName.ROLE_WAREHOUSE_STAFF.name()));

        SupportTicketResponse response = ticketService.getTicketById(principal.getId(), isStaff, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Append a new reply or internal note to ticket thread")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddTicketMessageRequest request) {
        boolean isStaff = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()) ||
                           a.getAuthority().equals(RoleName.ROLE_SUPER_ADMIN.name()));

        boolean isSeller = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_SELLER.name()));

        SenderRole role = isStaff ? SenderRole.SUPPORT_AGENT : (isSeller ? SenderRole.SELLER : SenderRole.CUSTOMER);
        TicketMessageResponse response = ticketService.addMessage(principal.getId(), role, id, request);
        return ResponseEntity.ok(ApiResponse.success("Message sent", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update ticket status (e.g. IN_PROGRESS, RESOLVED, CLOSED)")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        SupportTicketResponse response = ticketService.updateTicketStatus(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Status updated", response));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Assign ticket to a support agent")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assignTicket(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTicketRequest request) {
        SupportTicketResponse response = ticketService.assignTicket(id, request.getAgentId());
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List all customer support tickets with status filter")
    public ResponseEntity<ApiResponse<PageResponse<SupportTicketResponse>>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SupportTicketResponse> response = ticketService.getAllTickets(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
