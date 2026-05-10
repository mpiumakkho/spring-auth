package com.mp.core.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.RefreshToken;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.repository.RefreshTokenRepository;
import com.mp.core.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Value("${core.security.refresh-token.ttl-days:7}")
    private int ttlDays;

    public RefreshTokenServiceImpl(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public RefreshToken issue(String userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(plusDays(new Date(), ttlDays));
        token.setRevoked(false);
        return repo.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        RefreshToken stored = repo.findByToken(token)
                .orElseThrow(() -> new BusinessValidationException("Invalid refresh token"));
        if (stored.isRevoked()) {
            throw new BusinessValidationException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt() == null || stored.getExpiresAt().before(new Date())) {
            throw new BusinessValidationException("Refresh token has expired");
        }
        return stored;
    }

    @Override
    @Transactional
    public void revoke(String token) {
        repo.findByToken(token).ifPresent(t -> {
            t.setRevoked(true);
            repo.save(t);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        repo.revokeAllByUserId(userId);
    }

    private static Date plusDays(Date base, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}
