package com.mp.core.security;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mp.core.entity.Permission;
import com.mp.core.entity.Role;
import com.mp.core.entity.User;
import com.mp.core.entity.UserSession;
import com.mp.core.repository.UserRepository;
import com.mp.core.repository.UserSessionRepository;
import com.mp.core.service.UserSessionService;

import org.springframework.lang.NonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Token-based authentication filter for Core API.
 * Validates session tokens and sets up Spring Security authentication context.
 * 
 * Flow:
 * 1. Extract token from Authorization header (Bearer token)
 * 2. Validate token using UserSessionService
 * 3. Load user with roles and permissions
 * 4. Create Authentication object with authorities
 * 5. Set authentication in SecurityContext
 * 6. Update session activity timestamp
 */
@Slf4j
@Component
public class TokenFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final UserSessionService sessionService;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Value("${app.security.api-key}")
    private String apiKey;

    public TokenFilter(
            UserSessionService sessionService,
            UserSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
        this.sessionService = sessionService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Validate API key for all /api/** requests
        if (path.startsWith("/api/")) {
            String requestApiKey = request.getHeader(API_KEY_HEADER);
            if (requestApiKey == null || !java.security.MessageDigest.isEqual(
                    requestApiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    apiKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
                return;
            }
        }

        try {
            String token = extractToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateToken(token, request);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract token from Authorization header
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Authenticate user by token and set up security context
     */
    private void authenticateToken(String token, HttpServletRequest request) {
        // Validate session
        if (!sessionService.isSessionValid(token)) {
            log.debug("Invalid or expired token");
            return;
        }

        // Get session and user
        UserSession session = sessionRepository.findByToken(token).orElse(null);
        if (session == null) {
            log.warn("Session not found for valid token");
            return;
        }

        String userId = session.getUserId();
        if (userId == null) {
            log.warn("Session has no userId: sessionId={}", session.getSessionId());
            return;
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for session: userId={}", userId);
            return;
        }

        // Check user status
        if (!"active".equals(user.getStatus())) {
            log.warn("User is not active: userId={}, status={}", user.getUserId(), user.getStatus());
            return;
        }

        // Build authorities from roles and permissions
        List<GrantedAuthority> authorities = buildAuthorities(user);

        // Create authentication token
        String username = user.getUsername() != null ? user.getUsername() : "";
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            username,
            null,
            authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.debug("User authenticated: username={}, authorities={}", 
            user.getUsername(), authorities.size());

        // Update session activity
        sessionService.updateActivity(token);
    }

    /**
     * Build Spring Security authorities from user's roles and permissions
     * 
     * Creates authorities in two formats:
     * - ROLE_{roleName} - for hasRole() checks
     * - PERM_{resource}:{action} - for hasPermission() checks
     */
    private List<GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Add role authority (for @PreAuthorize("hasRole('ADMIN')"))
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
                
                // Add permissions from role (for @PreAuthorize("hasPermission()"))
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        String permissionKey = String.format("PERM_%s:%s", 
                            permission.getResource().toUpperCase(),
                            permission.getAction().toUpperCase()
                        );
                        authorities.add(new SimpleGrantedAuthority(permissionKey));
                    }
                }
            }
        }

        return new ArrayList<>(authorities);
    }

    /**
     * Skip filter for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip authentication for public endpoints
        return path.startsWith("/actuator/health") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
