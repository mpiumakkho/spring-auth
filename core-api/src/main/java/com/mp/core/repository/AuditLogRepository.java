package com.mp.core.repository;

import java.util.Date;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mp.core.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:actor IS NULL OR a.actor = :actor)
              AND (:action IS NULL OR a.action = :action)
              AND (:targetType IS NULL OR a.targetType = :targetType)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            """)
    Page<AuditLog> search(
            @Param("actor") String actor,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("from") Date from,
            @Param("to") Date to,
            Pageable pageable);
}
