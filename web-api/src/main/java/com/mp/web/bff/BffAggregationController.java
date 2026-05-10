package com.mp.web.bff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF aggregation surface. The dashboard endpoint replaces 3 separate
 * round-trips from the SPA with a single call that fans out in parallel:
 *
 *   GET /api/v1/users/me
 *   GET /api/v1/notifications/unread-count
 *   GET /api/v1/notifications?page=0&size=5
 *
 * Net effect: one BFF hop instead of three SPA hops, three parallel
 * core-api calls instead of three sequential ones.
 */
@Slf4j
@RestController
@RequestMapping("/bff/api/v1")
public class BffAggregationController {

    private final RestTemplate restTemplate;
    private final CookieHelper cookies;
    private final Timer aggregationTimer;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Value("${core.api.key}")
    private String coreApiKey;

    public BffAggregationController(RestTemplate restTemplate, CookieHelper cookies, MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.cookies = cookies;
        this.aggregationTimer = Timer.builder("bff.aggregation.duration")
                .description("Duration of BFF aggregation endpoints")
                .tag("endpoint", "me-full")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @GetMapping("/me/full")
    public ResponseEntity<Map<String, Object>> meFull(HttpServletRequest request)
            throws ExecutionException, InterruptedException {
        String jwt = cookies.readToken(request);
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Timer.Sample sample = Timer.start();
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(jwt);
            h.set("X-API-Key", coreApiKey);
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> entity = new HttpEntity<>(h);

            ParameterizedTypeReference<Map<String, Object>> mapType =
                    new ParameterizedTypeReference<>() {};

            CompletableFuture<Map<String, Object>> me = CompletableFuture.supplyAsync(() ->
                    restTemplate.exchange(coreApiUrl + "/api/v1/users/me",
                            org.springframework.http.HttpMethod.GET, entity, mapType).getBody());

            CompletableFuture<Long> unread = CompletableFuture.supplyAsync(() -> {
                ResponseEntity<Long> r = restTemplate.exchange(
                        coreApiUrl + "/api/v1/notifications/unread-count",
                        org.springframework.http.HttpMethod.GET, entity, Long.class);
                return r.getBody() == null ? 0L : r.getBody();
            });

            CompletableFuture<Map<String, Object>> recent = CompletableFuture.supplyAsync(() ->
                    restTemplate.exchange(coreApiUrl + "/api/v1/notifications?page=0&size=5",
                            org.springframework.http.HttpMethod.GET, entity, mapType).getBody());

            CompletableFuture.allOf(me, unread, recent).get();

            Map<String, Object> body = new HashMap<>();
            body.put("user", me.get());
            body.put("unreadNotifications", unread.get());
            body.put("recentNotifications", recent.get());
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.warn("Aggregation /me/full failed: {}", e.getMessage());
            throw e;
        } finally {
            sample.stop(aggregationTimer);
        }
    }
}
