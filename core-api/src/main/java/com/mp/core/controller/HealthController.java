package com.mp.core.controller;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthInfo() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        try {
            healthInfo.put("status", "UP");
            healthInfo.put("application", "RBAC Core API");
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("database", checkDatabaseHealth());
            healthInfo.put("system", getSystemInfo());
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            healthInfo.put("status", "DOWN");
            healthInfo.put("application", "RBAC Core API");
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("error", "Health check failed: " + e.getMessage());
            return ResponseEntity.status(503).body(healthInfo);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        try {
            checkDatabaseConnection();
            return ResponseEntity.ok("RBAC Core API - Status: OK - Database: Connected");
        } catch (Exception e) {
            return ResponseEntity.status(503).body("RBAC Core API - Status: ERROR - Database connection failed: " + e.getMessage());
        }
    }

    @Override
    public Health health() {
        try {
            checkDatabaseConnection();
            return Health.up()
                    .withDetail("application", "RBAC Core API")
                    .withDetail("database", "PostgreSQL - Connected")
                    .withDetail("timestamp", System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("application", "RBAC Core API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            checkDatabaseConnection();
            dbHealth.put("status", "UP");
            dbHealth.put("database", "PostgreSQL");
            dbHealth.put("validationQuery", "SELECT 1");
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        return dbHealth;
    }

    private void checkDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(10)) {
                throw new Exception("Database connection is not valid");
            }
        }
    }

    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("totalMemory", runtime.totalMemory());
        systemInfo.put("freeMemory", runtime.freeMemory());
        systemInfo.put("maxMemory", runtime.maxMemory());
        systemInfo.put("processors", runtime.availableProcessors());
        
        return systemInfo;
    }
} 