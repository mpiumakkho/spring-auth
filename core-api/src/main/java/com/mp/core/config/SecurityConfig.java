package com.mp.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mp.core.security.CustomPermissionEvaluator;
import com.mp.core.security.RateLimitFilter;
import com.mp.core.security.TokenFilter;

/**
 * Spring Security configuration for Core API.
 * 
 * Features:
 * - Token-based authentication (stateless)
 * - Method security with custom permission evaluator
 * - RBAC (Role-Based Access Control)
 * - Fine-grained permission checks
 * 
 * Security Components:
 * - TokenFilter: Validates tokens and sets authentication
 * - CustomPermissionEvaluator: Evaluates @PreAuthorize permissions
 * - BCryptPasswordEncoder: Password hashing
 */
@Configuration
@EnableMethodSecurity  // Enable @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    private final TokenFilter tokenFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomPermissionEvaluator permissionEvaluator;

    public SecurityConfig(
            TokenFilter tokenFilter,
            RateLimitFilter rateLimitFilter,
            CustomPermissionEvaluator permissionEvaluator
    ) {
        this.tokenFilter = tokenFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Password encoder for hashing passwords.
     * Uses BCrypt algorithm with default strength (10).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A, 12);
    }

    /**
     * Register CustomPermissionEvaluator for hasPermission() expressions.
     * This enables @PreAuthorize("hasPermission(null, 'USER:READ')") to work.
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = 
            new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }

    /**
     * Security filter chain configuration.
     * 
     * Features:
     * - CSRF disabled (stateless API)
     * - Stateless session management
     * - Token-based authentication via TokenFilter
     * - Public endpoints for health checks, docs, and authentication
     * - All other endpoints require authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session - don't create HTTP sessions
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/actuator/health",           // Health check
                    "/actuator/health/**",
                    "/api-docs/**",               // OpenAPI docs
                    "/swagger-ui/**",             // Swagger UI
                    "/swagger-ui.html",
                    "/v3/api-docs/**"             // OpenAPI v3 docs
                ).permitAll()

                // Authentication & session endpoints - public (protected by API key in TokenFilter)
                .requestMatchers(
                    "/api/users/login",
                    "/api/users/login-encrypt",
                    "/api/sessions/validate",
                    "/api/sessions/keep-alive",
                    "/api/sessions/logout"
                ).permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add rate limit filter first, then token filter
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tokenFilter, RateLimitFilter.class);
        
        return http.build();
    }
}
 