package com.mp.web.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mp.web.security.CoreApiAuthProvider;
import com.mp.web.security.SessionAuthSuccessHandler;
import com.mp.web.security.SessionFilter;

/**
 * Two filter chains so the BFF (cookie-JWT for SPA) and the legacy Thymeleaf MVC
 * (Spring Session form-login) can coexist on the same web-api instance.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CoreApiAuthProvider coreApiAuthProvider;

    @Autowired
    private SessionAuthSuccessHandler sessionAuthSuccessHandler;

    @Autowired
    private SessionFilter sessionFilter;

    @Value("${bff.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    /** BFF chain: stateless, CSRF off, CORS on. SPA hits /bff/**. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain bffFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/bff/**")
            .cors(cors -> cors.configurationSource(bffCorsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/bff/auth/login", "/bff/auth/refresh").permitAll()
                .anyRequest().permitAll() // BffProxyController enforces auth via cookie itself
            )
            .formLogin(Customizer.withDefaults()).formLogin(login -> login.disable())
            .httpBasic(b -> b.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource bffCorsSource() {
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

    /** Legacy Thymeleaf MVC chain — unchanged. */
    @Bean
    public SecurityFilterChain mvcFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/auth/logout")
                )
                .authenticationProvider(coreApiAuthProvider)
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/auth/login", "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/ums/css/**", "/ums/js/**", "/ums/images/**", "/ums/static/**").permitAll()
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/users/create", "/users/edit/**", "/users/update", "/users/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                .requestMatchers("/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER", "MODERATOR", "VIEWER", "SUPPORT")
                .requestMatchers("/roles/create", "/roles/edit/**", "/roles/update", "/roles/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "VIEWER")
                .requestMatchers("/permissions/create", "/permissions/edit/**", "/permissions/update", "/permissions/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/permissions/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "VIEWER")
                .requestMatchers("/reports/export", "/reports/analytics").hasAnyRole("SUPER_ADMIN", "ANALYST")
                .requestMatchers("/reports/**").hasAnyRole("SUPER_ADMIN", "ANALYST", "VIEWER", "SUPPORT")
                .requestMatchers("/dashboard", "/profile/**").authenticated()
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/")
                .successHandler(sessionAuthSuccessHandler)
                .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                );
        return http.build();
    }
}
