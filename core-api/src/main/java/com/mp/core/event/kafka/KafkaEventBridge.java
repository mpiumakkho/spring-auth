package com.mp.core.event.kafka;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.mp.core.event.DomainEvents;

/**
 * Republishes in-process DomainEvents to Kafka topics so external consumers
 * (analytics, search indexer, audit warehouse, ...) can subscribe.
 *
 * Activates only when `spring.kafka.bootstrap-servers` is configured — without
 * a broker, Spring's auto-config doesn't create a KafkaTemplate and this bean
 * stays out of the context. The in-process @Async listeners
 * ({@link com.mp.core.event.DomainEventListeners}) continue to handle the
 * write path either way.
 *
 * Topics:
 *   rbac-ums.audit
 *   rbac-ums.notifications
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class KafkaEventBridge {

    public static final String TOPIC_AUDIT = "rbac-ums.audit";
    public static final String TOPIC_NOTIFICATIONS = "rbac-ums.notifications";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventBridge(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async("eventExecutor")
    @EventListener
    public void onAudit(DomainEvents.AuditEvent e) {
        try {
            String key = e.targetType() + ":" + (e.targetId() == null ? "" : e.targetId());
            kafkaTemplate.send(TOPIC_AUDIT, key, e);
        } catch (Exception ex) {
            log.error("Kafka audit publish failed: {}", ex.getMessage());
        }
    }

    @Async("eventExecutor")
    @EventListener
    public void onNotification(DomainEvents.NotificationEvent e) {
        try {
            kafkaTemplate.send(TOPIC_NOTIFICATIONS, e.userId(), e);
        } catch (Exception ex) {
            log.error("Kafka notification publish failed: {}", ex.getMessage());
        }
    }
}
