package com.mp.core.service;

import com.mp.core.entity.RefreshToken;

public interface RefreshTokenService {

    RefreshToken issue(String userId);

    /**
     * Validate the supplied refresh token and return it. Throws on missing/expired/revoked.
     */
    RefreshToken validate(String token);

    void revoke(String token);

    void revokeAllForUser(String userId);
}
