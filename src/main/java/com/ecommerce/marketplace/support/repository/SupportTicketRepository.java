package com.ecommerce.marketplace.support.repository;

import com.ecommerce.marketplace.support.model.SupportTicket;
import com.ecommerce.marketplace.support.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    Page<SupportTicket> findByCustomerId(UUID customerId, Pageable pageable);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByAssignedAgentId(UUID agentId, Pageable pageable);

    List<SupportTicket> findByStatusInAndSlaDueAtBeforeAndIsEscalatedFalse(List<TicketStatus> statuses, Instant cutoff);
}
