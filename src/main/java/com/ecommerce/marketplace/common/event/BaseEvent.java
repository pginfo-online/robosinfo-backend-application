package com.ecommerce.marketplace.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all domain events published to Kafka.
 * All events carry correlation/causation IDs for distributed tracing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private UUID eventId;
    private String eventType;
    private int schemaVersion = 1;
    private UUID aggregateId;
    private String aggregateType;
    private Instant timestamp;
    private UUID correlationId;
    private UUID causationId;
    private Map<String, String> metadata;

    protected BaseEvent(String eventType, String aggregateType, UUID aggregateId) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
        this.schemaVersion = 1;
    }
}
