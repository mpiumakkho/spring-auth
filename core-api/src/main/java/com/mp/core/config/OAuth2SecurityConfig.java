package com.mp.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import com.mp.core.security.OAuth2SuccessHandler;

/**
 * Activates Spring's OAuth2 Login flow only when at least one provider is
 * configured (i.e. a ClientRegistrationRepository bean exists). Without
 * `spring.security.oauth2.client.registration.*` properties, this entire
 * config is skipped and the app starts normally with no OAuth2 routes.
 *
 * Production wiring example (env vars or application-prod.properties):
 *   spring.security.oauth2.client.registration.google.client-id=...
 *   spring.security.oauth2.client.registration.google.client-secret=...
 *   spring.security.oauth2.client.registration.github.client-id=...
 *   spring.security.oauth2.client.registration.github.client-secret=...
 *
 * SPA initiates login by redirecting to /oauth2/authorization/{provider}.
 */
@Configuration
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuth2SecurityConfig {

    private final OAuth2SuccessHandler successHandler;

    public OAuth2SecurityConfig(OAuth2SuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    @Order(0) // runs before SecurityConfig.filterChain (which has no @Order, default lowest)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/oauth2/**", "/login/oauth2/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .successHandler(successHandler)
                // No formLogin redirect on failure — let the SPA poll /me and discover.
                .failureUrl("/oauth2/failure")
            );
        return http.build();
    }
}
