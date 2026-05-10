package com.mp.web.bff;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic BFF proxy. Browser hits /bff/api/v1/<path>; this controller:
 *   1. Reads the JWT cookie (set by BffAuthController.login)
 *   2. Optionally serves a cached response for safe GETs on whitelisted paths
 *   3. Forwards to core-api at /api/v1/<path> with Authorization + X-API-Key
 *   4. Records duration in `bff.proxy.duration` for the BFF/core-api split
 *
 * Hides the X-API-Key from the SPA entirely. Returns 401 when the JWT cookie
 * is missing or upstream rejects it; the SPA can call /bff/auth/refresh and retry.
 */
@Slf4j
@Controller
@RequestMapping("/bff/api")
public class BffProxyController {

    private static final String BFF_PREFIX = "/bff/api";

    private final RestTemplate restTemplate;
    private final CookieHelper cookies;
    private final Cache responseCache;
    private final MeterRegistry meterRegistry;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Value("${core.api.key}")
    private String coreApiKey;

    @Value("#{'${bff.cache.cacheable-paths:}'.split(',')}")
    private List<String> cacheablePaths;

    public BffProxyController(
            RestTemplate restTemplate,
            CookieHelper cookies,
            CacheManager bffCacheManager,
            MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.cookies = cookies;
        this.responseCache = bffCacheManager.getCache("bffResponse");
        this.meterRegistry = registry;
    }

    private Timer timerFor(String method, String path, String outcome) {
        return Timer.builder("bff.proxy.duration")
                .description("BFF -> core-api proxy duration")
                .tag("method", method)
                .tag("path", path)
                .tag("outcome", outcome)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String token = cookies.readToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String path = stripContextAndBff(request);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        // Cache lookup for safe GETs on whitelisted paths
        String cacheKey = null;
        if (method == HttpMethod.GET && responseCache != null && isCacheable(path)) {
            cacheKey = cacheKey(token, path, request.getQueryString());
            CachedResponse hit = responseCache.get(cacheKey, CachedResponse.class);
            if (hit != null) {
                return ResponseEntity.status(hit.status())
                        .headers(hit.headers())
                        .header("X-BFF-Cache", "HIT")
                        .body(hit.body());
            }
        }

        URI target = UriComponentsBuilder.fromUriString(coreApiUrl)
                .path("/api" + path)
                .query(request.getQueryString())
                .build(true)
                .toUri();

        HttpHeaders headers = copyHeaders(request);
        headers.setBearerAuth(token);
        headers.set("X-API-Key", coreApiKey);
        headers.remove(HttpHeaders.COOKIE);

        byte[] body = request.getInputStream().readAllBytes();
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    target, method, new HttpEntity<>(body, headers), byte[].class);

            if (cacheKey != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                responseCache.put(cacheKey, new CachedResponse(
                        response.getStatusCode().value(),
                        filterResponseHeaders(response.getHeaders()),
                        response.getBody()));
            }
            return response;
        } catch (HttpStatusCodeException ex) {
            log.debug("Upstream {} {} -> {}", method, target, ex.getStatusCode());
            outcome = ex.getStatusCode().is4xxClientError() ? "client_error" : "server_error";
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(filterResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsByteArray());
        } finally {
            sample.stop(timerFor(method.name(), normalizeForTag(path), outcome));
        }
    }

    private boolean isCacheable(String path) {
        if (cacheablePaths == null) return false;
        for (String p : cacheablePaths) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty() && path.startsWith(trimmed)) return true;
        }
        return false;
    }

    private String cacheKey(String token, String path, String query) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(token.getBytes(StandardCharsets.UTF_8));
            String tokenHash = HexFormat.of().formatHex(md.digest()).substring(0, 16);
            return tokenHash + "|" + path + "|" + (query == null ? "" : query);
        } catch (Exception e) {
            return token + "|" + path + "|" + (query == null ? "" : query);
        }
    }

    private static String normalizeForTag(String path) {
        // collapse UUIDs and numeric ids in the metric tag so cardinality stays bounded
        return path
                .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }

    private String stripContextAndBff(HttpServletRequest request) {
        String full = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (full == null) full = request.getRequestURI().substring(request.getContextPath().length());
        if (full.startsWith(BFF_PREFIX)) full = full.substring(BFF_PREFIX.length());
        return full;
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Set<String> skip = Set.of(
                HttpHeaders.HOST.toLowerCase(),
                HttpHeaders.AUTHORIZATION.toLowerCase(),
                "x-api-key",
                HttpHeaders.CONTENT_LENGTH.toLowerCase(),
                HttpHeaders.COOKIE.toLowerCase());
        java.util.Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (skip.contains(name.toLowerCase())) continue;
            java.util.Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }
        return headers;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders src) {
        if (src == null) return new HttpHeaders();
        HttpHeaders dst = new HttpHeaders();
        src.forEach((k, v) -> {
            if (!k.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)
                    && !k.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                    && !k.equalsIgnoreCase("Connection")) {
                dst.addAll(k, v);
            }
        });
        return dst;
    }

    private record CachedResponse(int status, HttpHeaders headers, byte[] body) {}
}
