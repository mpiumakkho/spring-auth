package com.mp.core.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mp.core.entity.Permission;
import com.mp.core.entity.Role;
import com.mp.core.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTtlMillis;
    private final String issuer;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-ttl-minutes:30}") long accessTtlMinutes,
            @Value("${app.security.jwt.issuer:rbac-ums}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes; got " + bytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
        this.accessTtlMillis = accessTtlMinutes * 60_000L;
        this.issuer = issuer;
    }

    public String generate(User user) {
        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                roles.add(role.getName());
                if (role.getPermissions() != null) {
                    for (Permission p : role.getPermissions()) {
                        permissions.add(p.getResource().toUpperCase() + ":" + p.getAction().toUpperCase());
                    }
                }
            }
        }

        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTtlMillis);

        return Jwts.builder()
                .subject(user.getUserId())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(exp)
                .claim("username", user.getUsername())
                .claim("tid", user.getTenantId())
                .claim("roles", List.copyOf(roles))
                .claim("perms", List.copyOf(permissions))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    public long getAccessTtlMillis() {
        return accessTtlMillis;
    }
}
