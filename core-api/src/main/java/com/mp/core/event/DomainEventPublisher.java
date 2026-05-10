package com.mp.core.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's ApplicationEventPublisher. Services depend on this
 * facade (not on Spring's interface directly) so swapping to Kafka/RabbitMQ later
 * touches one class.
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public DomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void audit(String actor, String action, String targetType, String targetId, String detail) {
        publisher.publishEvent(new DomainEvents.AuditEvent(actor, action, targetType, targetId, detail));
    }

    public void notify(String userId, String type, String title, String message) {
        if (userId == null || userId.isBlank()) return;
        publisher.publishEvent(new DomainEvents.NotificationEvent(userId, type, title, message));
    }
}
