package com.mp.web.bff;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic BFF proxy. Browser hits /bff/api/v1/<path>; this controller:
 *   1. Reads the JWT cookie (set by BffAuthController.login)
 *   2. Forwards to core-api at /api/v1/<path> with Authorization + X-API-Key headers
 *   3. Streams the upstream response back
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

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Value("${core.api.key}")
    private String coreApiKey;

    public BffProxyController(RestTemplate restTemplate, CookieHelper cookies) {
        this.restTemplate = restTemplate;
        this.cookies = cookies;
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String token = cookies.readToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String path = stripContextAndBff(request);
        URI target = UriComponentsBuilder.fromUriString(coreApiUrl)
                .path("/api" + path)
                .query(request.getQueryString())
                .build(true)
                .toUri();

        HttpHeaders headers = copyHeaders(request);
        headers.setBearerAuth(token);
        headers.set("X-API-Key", coreApiKey);
        // remove cookie header so we don't leak our internal cookies upstream
        headers.remove(HttpHeaders.COOKIE);

        byte[] body = request.getInputStream().readAllBytes();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            return restTemplate.exchange(target, method, new HttpEntity<>(body, headers), byte[].class);
        } catch (HttpStatusCodeException ex) {
            log.debug("Upstream {} {} -> {}", method, target, ex.getStatusCode());
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(filterResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsByteArray());
        }
    }

    /**
     * Path the SPA hit minus the servlet context-path (`/ums`) minus the BFF
     * prefix (`/bff/api`). E.g. /ums/bff/api/v1/users -> /v1/users.
     */
    private String stripContextAndBff(HttpServletRequest request) {
        String full = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (full == null) full = request.getRequestURI().substring(request.getContextPath().length());
        if (full.startsWith(BFF_PREFIX)) full = full.substring(BFF_PREFIX.length());
        return full;
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        java.util.Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            // skip hop-by-hop + auth headers we'll set ourselves
            if (name.equalsIgnoreCase(HttpHeaders.HOST)
                    || name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)
                    || name.equalsIgnoreCase("X-API-Key")
                    || name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
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
}
