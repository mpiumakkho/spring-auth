package com.mp.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caches:
 *   - userRoles       keyed by userId          (per-user role list)
 *   - rolePermissions keyed by roleId          (per-role permission list)
 *
 * Backend selected by spring.cache.type — 'simple' (in-memory) for dev,
 * 'redis' for prod (spring-boot-starter-data-redis is on the classpath).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
