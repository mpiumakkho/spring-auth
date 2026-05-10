package com.mp.core.security;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple in-memory rate limiter for sensitive endpoints (login, session).
 * Limits requests per IP address within a sliding time window.
 * Includes periodic cleanup to prevent memory leaks from stale entries.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_IPS = 10_000;

    @Value("${app.security.rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${app.security.rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestCounts = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Hard cap: reject if too many IPs tracked (DDoS protection)
        if (requestCounts.size() >= MAX_TRACKED_IPS) {
            cleanup();
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        ConcurrentLinkedDeque<Long> timestamps = requestCounts.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        // Remove expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        timestamps.addLast(now);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/users/login") &&
               !path.equals("/api/users/login-encrypt") &&
               !path.startsWith("/api/sessions/");
    }

    private void cleanup() {
        long windowStart = System.currentTimeMillis() - (windowSeconds * 1000L);
        Iterator<Map.Entry<String, ConcurrentLinkedDeque<Long>>> it = requestCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConcurrentLinkedDeque<Long>> entry = it.next();
            ConcurrentLinkedDeque<Long> timestamps = entry.getValue();
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.isEmpty()) {
                it.remove();
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
