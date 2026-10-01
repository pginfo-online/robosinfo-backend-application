package com.ecommerce.marketplace.support.service;

import com.ecommerce.marketplace.support.model.SupportTicket;
import com.ecommerce.marketplace.support.model.TicketPriority;
import com.ecommerce.marketplace.support.model.TicketStatus;
import com.ecommerce.marketplace.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaEscalationScheduler {

    private final SupportTicketRepository ticketRepository;

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "checkTicketSlaEscalations", lockAtMostFor = "50s", lockAtLeastFor = "10s")
    public void checkSlaBreaches() {
        List<TicketStatus> activeStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        List<SupportTicket> breached = ticketRepository.findByStatusInAndSlaDueAtBeforeAndIsEscalatedFalse(
            activeStatuses, Instant.now()
        );

        if (breached.isEmpty()) {
            return;
        }

        for (SupportTicket ticket : breached) {
            try {
                escalateTicket(ticket);
            } catch (Exception e) {
                log.error("Failed to escalate ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            }
        }
    }

    @Transactional
    public void escalateTicket(SupportTicket ticket) {
        ticket.setIsEscalated(true);
        ticket.setPriority(TicketPriority.URGENT);
        ticketRepository.save(ticket);
        log.warn("SLA BREACH DETECTED: Ticket {} (ID: {}) escalated to URGENT priority",
            ticket.getTicketNumber(), ticket.getId());
    }
}
