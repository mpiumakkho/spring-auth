package com.mp.core.service;

import java.util.List;
import java.util.Optional;

import com.mp.core.entity.UserAttribute;

public interface UserAttributeService {
    UserAttribute set(String userId, String key, String value);
    Optional<String> get(String userId, String key);
    List<UserAttribute> all(String userId);
    void delete(String userId, String key);
}
