package com.mp.core.service;

import com.mp.core.entity.User;

public interface OAuthLinkService {

    /**
     * Find an existing user linked to (provider, providerUid). If none, look up by email
     * and link to that account, or auto-create a new ACTIVE user with no password.
     *
     * @return the resolved internal User
     */
    User resolveOrCreate(String provider, String providerUid, String email,
                        String firstName, String lastName, String avatarUrl);
}
