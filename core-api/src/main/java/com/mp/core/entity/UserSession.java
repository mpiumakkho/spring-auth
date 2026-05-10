package com.mp.core.entity;

import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_sessions", schema = "sample_app")
@EntityListeners(AuditingEntityListener.class)
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sessionId;
    
    private String userId;
    private String token;
    
    @Column(nullable = false)
    private String status = "active";
    
    @CreatedDate
    private Date createdAt;
    
    @LastModifiedDate
    private Date lastActivityAt;

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public boolean isActive() { return "active".equals(status); }
    public void setActive(boolean active) { this.status = active ? "active" : "inactive"; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Date lastActivityAt) { this.lastActivityAt = lastActivityAt; }
} 