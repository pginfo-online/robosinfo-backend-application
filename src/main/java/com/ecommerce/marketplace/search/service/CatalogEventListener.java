package com.ecommerce.marketplace.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event synchronizer listening to catalog changes for search indexing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class CatalogEventListener {

    @KafkaListener(topics = "marketplace.catalog", groupId = "search-synchronizer", autoStartup = "${app.kafka.enabled:false}")
    public void onCatalogEvent(String eventPayload) {
        log.info("Received catalog event for search index sync: {}", eventPayload);
        // Transform entity to SearchDocument and index into OpenSearch cluster when enabled
    }

    public void indexProduct(UUID productId) {
        log.debug("Index update triggered for product ID: {}", productId);
    }
}
