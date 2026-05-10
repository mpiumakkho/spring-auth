package com.mp.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.entity.AuditLog;
import com.mp.core.service.AuditService;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType) {

        List<AuditLog> logs;
        if (actor != null && !actor.isBlank()) {
            logs = auditService.getLogsByActor(actor);
        } else if (action != null && !action.isBlank()) {
            logs = auditService.getLogsByAction(action);
        } else if (targetType != null && !targetType.isBlank()) {
            logs = auditService.getLogsByTargetType(targetType);
        } else {
            logs = auditService.getRecentLogs();
        }

        return ResponseEntity.ok(logs);
    }
}
