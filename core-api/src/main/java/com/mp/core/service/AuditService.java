package com.mp.core.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            // Audit failure should never break business logic
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditRepo.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByActor(String actor) {
        return auditRepo.findByActorOrderByCreatedAtDesc(actor);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(String action) {
        return auditRepo.findByActionOrderByCreatedAtDesc(action);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTargetType(String targetType) {
        return auditRepo.findByTargetTypeOrderByCreatedAtDesc(targetType);
    }
}
