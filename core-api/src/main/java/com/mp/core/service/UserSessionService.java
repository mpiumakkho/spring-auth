package com.mp.core.service;

import com.mp.core.entity.UserSession;

public interface UserSessionService {
    UserSession createSession(String userId);
    void updateActivity(String token);
    void invalidateSession(String token);
    void invalidateUserSessions(String userId);
    boolean isSessionValid(String token);
    void cleanupInactiveSessions();
} 