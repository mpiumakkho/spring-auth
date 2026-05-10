package com.mp.web.bff;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF auth surface for the SPA. The browser only sees:
 *   - POST /bff/auth/login            {username, password} -> 200 + Set-Cookie
 *   - POST /bff/auth/logout           -> Clear-Cookie
 *   - POST /bff/auth/refresh          -> rotates the JWT cookie using the refresh cookie
 *   - GET  /bff/auth/oauth2-callback  -> consumes ?token=&refreshToken= from core-api OAuth2 success handler
 *
 * The X-API-Key never leaves this service. The JWT and refresh token are
 * httpOnly cookies, immune to JS/XSS read access.
 */
@Slf4j
@RestController
@RequestMapping("/bff/auth")
public class BffAuthController {

    private final RestTemplate restTemplate;
    private final CookieHelper cookies;
    private final ObjectMapper json;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Value("${core.api.key}")
    private String coreApiKey;

    @Value("${bff.spa.url:http://localhost:5173}")
    private String spaUrl;

    public BffAuthController(RestTemplate restTemplate, CookieHelper cookies, ObjectMapper json) {
        this.restTemplate = restTemplate;
        this.cookies = cookies;
        this.json = json;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) throws IOException {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("X-API-Key", coreApiKey);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(body, h);

            ResponseEntity<String> upstream = restTemplate.postForEntity(
                    coreApiUrl + "/api/v1/users/login", req, String.class);

            JsonNode root = json.readTree(upstream.getBody());
            JsonNode user = root.get("user");
            if (user == null || !user.isObject()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(upstream.getBody());
            }

            String jwt = textOrNull(user.get("token"));
            String refresh = textOrNull(user.get("refreshToken"));
            cookies.setTokenCookies(response, jwt, refresh);

            // Strip tokens before returning to the browser
            ObjectNode userClean = (ObjectNode) user.deepCopy();
            userClean.remove("token");
            userClean.remove("refreshToken");
            ObjectNode reply = json.createObjectNode();
            reply.set("user", userClean);
            return ResponseEntity.ok(json.writeValueAsString(reply));

        } catch (HttpClientErrorException ex) {
            log.warn("Upstream login failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String jwt = cookies.readToken(request);
        if (jwt != null) {
            try {
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                h.set("X-API-Key", coreApiKey);
                h.setBearerAuth(jwt);
                Map<String, String> body = Map.of("token", jwt);
                restTemplate.postForEntity(coreApiUrl + "/api/v1/sessions/logout",
                        new HttpEntity<>(body, h), Void.class);
            } catch (Exception ex) {
                log.debug("Upstream logout best-effort failed: {}", ex.getMessage());
            }
        }
        cookies.clearCookies(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String refresh = cookies.readRefreshToken(request);
        if (refresh == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set("X-API-Key", coreApiKey);
            Map<String, String> body = Map.of("refreshToken", refresh);
            ResponseEntity<String> upstream = restTemplate.postForEntity(
                    coreApiUrl + "/api/v1/sessions/refresh",
                    new HttpEntity<>(body, h), String.class);
            JsonNode root = json.readTree(upstream.getBody());
            cookies.setTokenCookies(response,
                    textOrNull(root.get("token")),
                    textOrNull(root.get("refreshToken")));
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException ex) {
            cookies.clearCookies(response);
            return ResponseEntity.status(ex.getStatusCode()).build();
        }
    }

    /**
     * Receives the redirect from core-api's OAuth2SuccessHandler, swaps the
     * token + refreshToken query params into httpOnly cookies, and bounces
     * the browser to the SPA root.
     */
    @GetMapping("/oauth2-callback")
    public ResponseEntity<Void> oauth2Callback(
            @RequestParam(name = "token") String token,
            @RequestParam(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        cookies.setTokenCookies(response, token, refreshToken);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, spaUrl + "/")
                .build();
    }

    private static String textOrNull(JsonNode node) {
        return (node == null || node.isNull()) ? null : node.asText();
    }
}
