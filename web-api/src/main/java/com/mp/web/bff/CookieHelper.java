package com.mp.web.bff;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Centralizes httpOnly cookie creation/reading for the BFF. The browser never sees
 * the JWT or refresh token directly; both live in cookies that are sent only on
 * same-origin requests to the BFF.
 */
@Component
public class CookieHelper {

    @Value("${bff.cookie.token-name:rbac_token}")
    private String tokenName;

    @Value("${bff.cookie.refresh-name:rbac_refresh}")
    private String refreshName;

    @Value("${bff.cookie.path:/}")
    private String path;

    @Value("${bff.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${bff.cookie.secure:false}")
    private boolean secure;

    @Value("${bff.cookie.token-ttl-seconds:1800}")
    private long tokenTtlSeconds;

    @Value("${bff.cookie.refresh-ttl-seconds:604800}")
    private long refreshTtlSeconds;

    public void setTokenCookies(HttpServletResponse response, String jwt, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(tokenName, jwt, tokenTtlSeconds).toString());
        if (refreshToken != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, build(refreshName, refreshToken, refreshTtlSeconds).toString());
        }
    }

    public void clearCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(tokenName, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, build(refreshName, "", 0).toString());
    }

    public String readToken(HttpServletRequest request) {
        return readCookie(request, tokenName);
    }

    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, refreshName);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private ResponseCookie build(String name, String value, long ttlSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(ttlSeconds))
                .build();
    }
}
