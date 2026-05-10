package com.mp.core.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByActorOrderByCreatedAtDesc(String actor);
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
    List<AuditLog> findByTargetTypeOrderByCreatedAtDesc(String targetType);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Date from, Date to);
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
