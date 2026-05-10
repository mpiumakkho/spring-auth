package com.mp.web.bff;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Short-TTL response cache for the BFF. Keys are scoped per-user (the JWT
 * cookie value is part of the key) so one user can never read another user's
 * cached response. TTL is intentionally tiny (default 20s) — enough to absorb
 * UI render bursts, not enough to mask role/permission changes.
 */
@Configuration
@EnableCaching
public class BffCacheConfig {

    @Value("${bff.cache.ttl-seconds:20}")
    private long ttlSeconds;

    @Value("${bff.cache.max-entries:2000}")
    private long maxEntries;

    @Bean
    public CacheManager bffCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager("bffResponse");
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxEntries)
                .recordStats());
        return mgr;
    }

    @Bean(name = "bffCacheTtlSeconds")
    public long bffCacheTtlSeconds() {
        return TimeUnit.SECONDS.toMillis(ttlSeconds);
    }
}
