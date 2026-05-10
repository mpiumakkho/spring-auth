package com.mp.core.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mp.core.entity.Tenant;
import com.mp.core.exception.BusinessValidationException;
import com.mp.core.exception.DuplicateResourceException;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.repository.TenantRepository;
import com.mp.core.service.TenantService;

@Service
public class TenantServiceImpl implements TenantService {

    private final TenantRepository repo;

    public TenantServiceImpl(TenantRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Tenant create(String name, String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessValidationException("Tenant slug is required");
        }
        String normalizedSlug = slug.trim().toLowerCase();
        if (repo.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException("Tenant", "slug", normalizedSlug);
        }
        Tenant t = new Tenant();
        t.setTenantId(java.util.UUID.randomUUID().toString());
        t.setName(name);
        t.setSlug(normalizedSlug);
        t.setStatus("active");
        return repo.save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant get(String tenantId) {
        return repo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getBySlug(String slug) {
        return repo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant with slug '" + slug + "' not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> all() {
        return repo.findAll();
    }

    @Override
    @Transactional
    public Tenant updateStatus(String tenantId, String status) {
        Tenant t = get(tenantId);
        t.setStatus(status);
        return repo.save(t);
    }
}
