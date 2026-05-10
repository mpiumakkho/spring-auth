package com.mp.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.UserAttribute;

public interface UserAttributeRepository extends JpaRepository<UserAttribute, String> {
    Optional<UserAttribute> findByUserIdAndKey(String userId, String key);
    List<UserAttribute> findByUserId(String userId);
}
