package com.mp.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.UserOAuthLink;

public interface UserOAuthLinkRepository extends JpaRepository<UserOAuthLink, String> {
    Optional<UserOAuthLink> findByProviderAndProviderUid(String provider, String providerUid);
}
