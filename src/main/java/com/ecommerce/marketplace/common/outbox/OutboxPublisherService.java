package com.ecommerce.marketplace.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "publishOutboxEvents", lockAtMostFor = "4s", lockAtLeastFor = "1s")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markPublished(event.getId());
                        } else {
                            log.error("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
                            markFailed(event.getId());
                        }
                    });
            } catch (Exception e) {
                log.error("Error dispatching outbox event {}: {}", event.getId(), e.getMessage());
                markFailed(event.getId());
            }
        }
    }

    @Transactional
    public void markPublished(java.util.UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markFailed(java.util.UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= 5) {
                event.setStatus("FAILED");
            }
            outboxEventRepository.save(event);
        });
    }
}
