package com.mp.core.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.AuditLog;
import com.mp.core.repository.AuditLogRepository;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditRepo;

    public AuditService(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actor, String action, String targetType, String targetId, String detail) {
        try {
            AuditLog entry = new AuditLog(actor, action, targetType, targetId, detail);
            auditRepo.save(entry);
        } catch (Exception e) {
            // Audit failure must never break the main business flow
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String actor, String action, String targetType, Date from, Date to, Pageable pageable) {
        String a = (actor != null && !actor.isBlank()) ? actor : null;
        String act = (action != null && !action.isBlank()) ? action : null;
        String t = (targetType != null && !targetType.isBlank()) ? targetType : null;
        return auditRepo.search(a, act, t, from, to, pageable);
    }
}
