package com.mp.core.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mp.core.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    Optional<UserSession> findByToken(String token);
    
    List<UserSession> findByUserId(String userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.status = 'active' AND s.lastActivityAt < :threshold")
    List<UserSession> findInactiveSessions(@Param("threshold") Date threshold);
    
    @Query("SELECT s FROM UserSession s WHERE s.status = 'active' AND s.userId = :userId")
    Optional<UserSession> findActiveSessionByUserId(@Param("userId") String userId);
} 