package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Notification;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.NotificationRepository;
import com.mp.core.service.NotificationService;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repo;

    public NotificationServiceImpl(NotificationRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Notification notify(String userId, String type, String title, String message) {
        if (userId == null || userId.isBlank()) {
            log.warn("Skipped notification with no userId: type={}, title={}", type, title);
            return null;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        return repo.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> list(String userId, Pageable pageable) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markRead(String userId, String notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new BusinessValidationException("Notification does not belong to current user");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(new Date());
            repo.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllRead(String userId) {
        Date now = new Date();
        repo.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .forEach(n -> {
                    if (n.getReadAt() == null) {
                        n.setReadAt(now);
                        repo.save(n);
                    }
                });
    }
}
