package com.mp.web.security;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionAuthSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();
        session.setAttribute("userId", authentication.getName());
        session.setAttribute("username", authentication.getName());
        session.setAttribute("roles", authentication.getAuthorities());
        response.sendRedirect(request.getContextPath() + "/dashboard");

        Collection<?> roles = (Collection<?>) session.getAttribute("roles");
        Set<String> allowedRoles = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_USER_MANAGER", "ROLE_VIEWER",
            "ROLE_MODERATOR", "ROLE_ANALYST", "ROLE_SUPPORT", "ROLE_USER"
        );
        if (roles != null && roles.stream().anyMatch(r -> allowedRoles.contains(r.toString()))) {
            // ผู้ใช้มี role ที่อยู่ใน allowedRoles
        }
    }
} 