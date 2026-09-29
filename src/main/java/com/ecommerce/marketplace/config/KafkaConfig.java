package com.ecommerce.marketplace.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String TOPIC_ORDERS = "marketplace.orders";
    public static final String TOPIC_PAYMENTS = "marketplace.payments";
    public static final String TOPIC_INVENTORY = "marketplace.inventory";
    public static final String TOPIC_NOTIFICATIONS = "marketplace.notifications";
    public static final String TOPIC_SELLERS = "marketplace.sellers";
    public static final String TOPIC_CATALOG = "marketplace.catalog";

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(TOPIC_ORDERS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic sellersTopic() {
        return TopicBuilder.name(TOPIC_SELLERS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic catalogTopic() {
        return TopicBuilder.name(TOPIC_CATALOG).partitions(3).replicas(1).build();
    }
}
