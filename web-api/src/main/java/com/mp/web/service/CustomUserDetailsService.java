// Deprecated: ใช้ CustomAuthenticationProvider แทนสำหรับการ login ด้วย core-api

package com.mp.web.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${core.api.url}")
    private String coreApiUrl;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        Map user = restTemplate.postForObject(coreApiUrl + "/api/v1/users/find-by-username-or-email", req, Map.class);

        // log response จาก core-api
        System.out.println("[DEBUG] core-api user response: " + user);

        if (user == null || user.get("username") == null) {
            throw new UsernameNotFoundException("User not found");
        }

        String password = (String) user.get("password");
        List<Map<String, Object>> roleObjs = null;
        try {
            roleObjs = (List<Map<String, Object>>) user.get("roles");
        } catch (Exception e) {
            System.out.println("[DEBUG] roles mapping error: " + e.getMessage());
        }
        List<String> validRoles = List.of("SUPER_ADMIN", "ADMIN", "USER_MANAGER", "VIEWER", "MODERATOR", "ANALYST", "SUPPORT");
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleObjs != null) {
            for (Map<String, Object> roleObj : roleObjs) {
                String roleName = (String) roleObj.get("name");
                if (roleName != null && validRoles.contains(roleName)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                }
            }
        } else {
            System.out.println("[DEBUG] roles is null or not a list");
        }

        return new org.springframework.security.core.userdetails.User(
            (String) user.get("username"),
            password,
            authorities
        );
    }
} 