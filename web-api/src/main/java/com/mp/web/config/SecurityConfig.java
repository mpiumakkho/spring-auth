package com.mp.web.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * web-api is a pure BFF for the React SPA. The legacy Thymeleaf MVC chain
 * (form-login, Spring Session, server-rendered templates) was retired — the SPA
 * owns every UI surface now. All routes here are stateless cookie-JWT.
 *
 * Surfaces:
 *   - /bff/auth/login    /bff/auth/refresh    /bff/auth/oauth2-callback   public
 *   - everything else under /bff/**                                       BffProxyController
 *                                                                         + BffAuthController
 *                                                                         enforce auth via cookie
 *   - /actuator/**                                                        Prometheus + health
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${bff.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain bffFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/bff/auth/login", "/bff/auth/refresh", "/bff/auth/oauth2-callback").permitAll()
                // The BFF controllers enforce auth via the rbac_token cookie themselves —
                // Spring Security shouldn't 401 before they get to inspect it.
                .anyRequest().permitAll()
            )
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Content-Type", "Accept", "X-Requested-With"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/bff/**", cfg);
        return src;
    }
}
