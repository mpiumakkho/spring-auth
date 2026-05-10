package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.UserSession;
import com.mp.core.repository.UserSessionRepository;
import com.mp.core.service.UserSessionService;

@Slf4j
@Service
public class UserSessionServiceImpl implements UserSessionService {    
    @Value("${core.session.timeout-minutes:30}")
    private long sessionTimeoutMinutes;
    
    private final UserSessionRepository sessionRepo;
    
    public UserSessionServiceImpl(UserSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }
    
    @Override
    @Transactional
    public UserSession createSession(String userId) {
        // invalidate any existing sessions
        invalidateUserSessions(userId);
        
        // create new session
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setToken(UUID.randomUUID().toString());
        session.setStatus("active");
        
        log.info("creating new session for user: {}", userId);
        return sessionRepo.save(session);
    }
    
    @Override
    @Transactional
    public void updateActivity(String token) {
        sessionRepo.findByToken(token).ifPresent(session -> {
            session.setLastActivityAt(new Date());
            sessionRepo.save(session);
            log.debug("updated activity timestamp for session: {}", session.getSessionId());
        });
    }
    
    @Override
    @Transactional
    public void invalidateSession(String token) {
        sessionRepo.findByToken(token).ifPresent(session -> {
            session.setStatus("inactive");
            sessionRepo.save(session);
            log.info("invalidated session: {}", session.getSessionId());
        });
    }
    
    @Override
    @Transactional
    public void invalidateUserSessions(String userId) {
        List<UserSession> sessions = sessionRepo.findByUserId(userId);
        sessions.forEach(session -> {
            session.setStatus("inactive");
            sessionRepo.save(session);
        });
        if (!sessions.isEmpty()) {
            log.info("invalidated {} sessions for user: {}", sessions.size(), userId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isSessionValid(String token) {
        return sessionRepo.findByToken(token)
            .map(session -> {
                if (!"active".equals(session.getStatus())) {
                    return false;
                }
                
                Date threshold = getTimeoutThreshold();
                return session.getLastActivityAt().after(threshold);
            })
            .orElse(false);
    }
    
    @Override
    @Scheduled(fixedRateString = "${core.session.cleanup-interval:300000}")
    @Transactional
    public void cleanupInactiveSessions() {
        Date threshold = getTimeoutThreshold();
        List<UserSession> inactiveSessions = sessionRepo.findInactiveSessions(threshold);
        
        if (!inactiveSessions.isEmpty()) {
            inactiveSessions.forEach(session -> {
                session.setStatus("inactive");
                sessionRepo.save(session);
            });
            log.info("cleaned up {} inactive sessions", inactiveSessions.size());
        }
    }
    
    private Date getTimeoutThreshold() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -((int) sessionTimeoutMinutes));
        return cal.getTime();
    }
} 