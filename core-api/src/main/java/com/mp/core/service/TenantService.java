package com.mp.core.service;

import java.util.List;

import com.mp.core.entity.Tenant;

public interface TenantService {
    Tenant create(String name, String slug);
    Tenant get(String tenantId);
    Tenant getBySlug(String slug);
    List<Tenant> all();
    Tenant updateStatus(String tenantId, String status);
}
