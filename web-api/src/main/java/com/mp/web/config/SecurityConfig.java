package com.mp.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.mp.web.security.CoreApiAuthProvider;
import com.mp.web.security.SessionAuthSuccessHandler;
import com.mp.web.security.SessionFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CoreApiAuthProvider coreApiAuthProvider;

    @Autowired
    private SessionAuthSuccessHandler sessionAuthSuccessHandler;

    @Autowired
    private SessionFilter sessionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF enabled (default) — Thymeleaf th:action auto-includes CSRF token
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/auth/logout")
                )
                .authenticationProvider(coreApiAuthProvider)
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/auth/login", "/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/demo/css/**", "/demo/js/**", "/demo/images/**", "/demo/static/**").permitAll()
                // admin area
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                // user management
                .requestMatchers("/users/create", "/users/edit/**", "/users/update", "/users/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                .requestMatchers("/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER", "MODERATOR", "VIEWER", "SUPPORT")
                // role management
                .requestMatchers("/roles/create", "/roles/edit/**", "/roles/update", "/roles/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/roles/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "VIEWER")
                // permission management
                .requestMatchers("/permissions/create", "/permissions/edit/**", "/permissions/update", "/permissions/delete/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/permissions/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "VIEWER")
                // reports
                .requestMatchers("/reports/export", "/reports/analytics").hasAnyRole("SUPER_ADMIN", "ANALYST")
                .requestMatchers("/reports/**").hasAnyRole("SUPER_ADMIN", "ANALYST", "VIEWER", "SUPPORT")
                // dashboard and profile
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
