package com.mp.core.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.UserAttribute;
import com.mp.core.repository.UserAttributeRepository;
import com.mp.core.service.UserAttributeService;

@Service
public class UserAttributeServiceImpl implements UserAttributeService {

    private final UserAttributeRepository repo;

    public UserAttributeServiceImpl(UserAttributeRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userAttributes", key = "#userId + ':' + #key")
    public UserAttribute set(String userId, String key, String value) {
        UserAttribute attr = repo.findByUserIdAndKey(userId, key).orElseGet(() -> {
            UserAttribute a = new UserAttribute();
            a.setUserId(userId);
            a.setKey(key);
            return a;
        });
        attr.setValue(value);
        return repo.save(attr);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userAttributes", key = "#userId + ':' + #key")
    public Optional<String> get(String userId, String key) {
        return repo.findByUserIdAndKey(userId, key).map(UserAttribute::getValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAttribute> all(String userId) {
        return repo.findByUserId(userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "userAttributes", key = "#userId + ':' + #key")
    public void delete(String userId, String key) {
        repo.findByUserIdAndKey(userId, key).ifPresent(repo::delete);
    }
}
