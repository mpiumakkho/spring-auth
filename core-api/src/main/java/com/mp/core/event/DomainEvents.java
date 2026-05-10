package com.mp.core.event;

/**
 * Domain events fired by services when something user-visible happens.
 * Listeners (audit log, notifications) consume these asynchronously so the
 * main request flow never waits on cross-cutting writes.
 */
public final class DomainEvents {

    private DomainEvents() {}

    public record AuditEvent(
            String actor,
            String action,
            String targetType,
            String targetId,
            String detail) {}

    public record NotificationEvent(
            String userId,
            String type,
            String title,
            String message) {}
}
