package com.mp.web.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

@Component
public class CoreApiAuthProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CoreApiAuthProvider.class);

    private final RestTemplate restTemplate;
    private final String coreApiUrl;

    public CoreApiAuthProvider(
            RestTemplate restTemplate,
            @Value("${core.api.url}") String coreApiUrl) {
        this.restTemplate = restTemplate;
        this.coreApiUrl = coreApiUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> req = Map.of("username", username, "password", password);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<Map<String, Object>> respEntity = restTemplate.exchange(
                    coreApiUrl + "/api/users/login",
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> resp = respEntity.getBody();

            if (resp != null && Boolean.TRUE.equals(resp.get("success"))) {
                Map<String, Object> user = (Map<String, Object>) resp.get("user");
                if (user != null) {
                    storeSessionToken(user);
                    List<SimpleGrantedAuthority> authorities = extractAuthorities(user);
                    // Pass null instead of password to avoid keeping credentials in memory
                    return new UsernamePasswordAuthenticationToken(username, null, authorities);
                }
            }
        } catch (HttpClientErrorException e) {
            log.warn("Authentication failed for user '{}': HTTP {}", username, e.getStatusCode());
        } catch (Exception e) {
            log.error("Authentication error for user '{}': {}", username, e.getMessage());
        }

        throw new BadCredentialsException("Invalid username/email or password");
    }

    private void storeSessionToken(Map<String, Object> user) {
        String token = (String) user.get("token");
        if (token != null) {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(true);
            session.setAttribute("sessionToken", token);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities(Map<String, Object> user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<Map<String, Object>> roleObjs = (List<Map<String, Object>>) user.get("roles");
        if (roleObjs == null) {
            return authorities;
        }

        for (Map<String, Object> roleObj : roleObjs) {
            String roleName = (String) roleObj.get("name");
            if (roleName != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }
        return authorities;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
} 