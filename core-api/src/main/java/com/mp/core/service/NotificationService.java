package com.mp.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mp.core.entity.Notification;

public interface NotificationService {

    /** Fire-and-forget notification dispatch. Implementation runs on the event executor pool. */
    void notify(String userId, String type, String title, String message);

    Page<Notification> list(String userId, Pageable pageable);

    long unreadCount(String userId);

    void markRead(String userId, String notificationId);

    void markAllRead(String userId);
}
