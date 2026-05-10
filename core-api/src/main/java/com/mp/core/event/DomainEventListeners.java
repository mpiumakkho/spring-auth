package com.mp.core.event;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.mp.core.service.AuditService;
import com.mp.core.service.NotificationService;

/**
 * Async consumers. Both run on the eventExecutor pool so failures inside one
 * listener never bubble back to the originating HTTP request.
 */
@Slf4j
@Component
public class DomainEventListeners {

    private final AuditService auditService;
    private final NotificationService notificationService;

    public DomainEventListeners(AuditService auditService, NotificationService notificationService) {
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Async("eventExecutor")
    @EventListener
    public void onAudit(DomainEvents.AuditEvent e) {
        try {
            auditService.log(e.actor(), e.action(), e.targetType(), e.targetId(), e.detail());
        } catch (Exception ex) {
            log.error("Async audit listener failed: {}", ex.getMessage());
        }
    }

    @Async("eventExecutor")
    @EventListener
    public void onNotification(DomainEvents.NotificationEvent e) {
        try {
            notificationService.notify(e.userId(), e.type(), e.title(), e.message());
        } catch (Exception ex) {
            log.error("Async notification listener failed: {}", ex.getMessage());
        }
    }
}
