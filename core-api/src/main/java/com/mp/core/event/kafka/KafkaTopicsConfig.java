package com.mp.core.event.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Auto-create the topics we publish to. Spring Kafka's {@code KafkaAdmin} runs
 * during startup and creates them if missing (idempotent). Skipped when the
 * broker isn't configured.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class KafkaTopicsConfig {

    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name(KafkaEventBridge.TOPIC_AUDIT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(KafkaEventBridge.TOPIC_NOTIFICATIONS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
