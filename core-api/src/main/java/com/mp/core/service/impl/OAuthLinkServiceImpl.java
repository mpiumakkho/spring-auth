package com.mp.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.User;
import com.mp.core.entity.UserOAuthLink;
import com.mp.core.entity.UserStatus;
import com.mp.core.repository.UserOAuthLinkRepository;
import com.mp.core.repository.UserRepository;
import com.mp.core.service.AuditService;
import com.mp.core.service.OAuthLinkService;

@Slf4j
@Service
public class OAuthLinkServiceImpl implements OAuthLinkService {

    private final UserRepository userRepo;
    private final UserOAuthLinkRepository linkRepo;
    private final AuditService auditService;

    public OAuthLinkServiceImpl(UserRepository userRepo, UserOAuthLinkRepository linkRepo, AuditService auditService) {
        this.userRepo = userRepo;
        this.linkRepo = linkRepo;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public User resolveOrCreate(String provider, String providerUid, String email,
                                String firstName, String lastName, String avatarUrl) {

        // 1. Existing OAuth link?
        return linkRepo.findByProviderAndProviderUid(provider, providerUid)
                .map(link -> userRepo.findById(link.getUserId()).orElseThrow())
                // 2. Existing user with same email? Attach a link.
                .or(() -> {
                    if (email == null || email.isBlank()) return java.util.Optional.empty();
                    return userRepo.findByEmail(email).map(u -> {
                        UserOAuthLink link = new UserOAuthLink();
                        link.setUserId(u.getUserId());
                        link.setProvider(provider);
                        link.setProviderUid(providerUid);
                        link.setEmail(email);
                        linkRepo.save(link);
                        auditService.log(u.getUsername(), "OAUTH_LINK", "USER", u.getUserId(),
                                "Linked " + provider + " account uid=" + providerUid);
                        return u;
                    });
                })
                // 3. Auto-create.
                .orElseGet(() -> autoCreate(provider, providerUid, email, firstName, lastName, avatarUrl));
    }

    private User autoCreate(String provider, String providerUid, String email,
                            String firstName, String lastName, String avatarUrl) {
        String username = email != null ? email : (provider + "_" + providerUid);
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setAvatarUrl(avatarUrl);
        u.setStatus(UserStatus.ACTIVE);
        // No password for OAuth-only accounts; password reset can be used to set one later
        u.setPassword(null);
        User saved = userRepo.save(u);

        UserOAuthLink link = new UserOAuthLink();
        link.setUserId(saved.getUserId());
        link.setProvider(provider);
        link.setProviderUid(providerUid);
        link.setEmail(email);
        linkRepo.save(link);

        auditService.log(username, "OAUTH_AUTOCREATE", "USER", saved.getUserId(),
                "Auto-created via " + provider);
        log.info("Auto-created user via OAuth: provider={}, username={}", provider, username);
        return saved;
    }
}
