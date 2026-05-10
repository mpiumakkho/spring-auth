package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.OAuthExchangeRequestDTO;
import com.mp.core.entity.User;
import com.mp.core.entity.UserSession;
import com.mp.core.security.JwtService;
import com.mp.core.service.OAuthLinkService;
import com.mp.core.service.UserSessionService;

import jakarta.validation.Valid;

/**
 * BYO-identity OAuth bridge. The frontend obtains the user info from a provider
 * (Google, GitHub, Azure AD) via the provider's JS SDK, then POSTs verified claims
 * to /api/oauth2/callback/exchange. The server links/auto-creates the internal user
 * and returns a JWT + refresh token pair.
 *
 * Trust model: the X-API-Key already gates this endpoint. For production deployments
 * this controller should validate the provider's id_token signature itself.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2/callback")
public class OAuthController {

    private final OAuthLinkService linkService;
    private final UserSessionService sessionService;
    private final JwtService jwtService;

    public OAuthController(OAuthLinkService linkService, UserSessionService sessionService, JwtService jwtService) {
        this.linkService = linkService;
        this.sessionService = sessionService;
        this.jwtService = jwtService;
    }

    @PostMapping("/exchange")
    public ResponseEntity<String> exchange(@Valid @RequestBody OAuthExchangeRequestDTO req) {
        User user = linkService.resolveOrCreate(
                req.provider(), req.providerUid(), req.email(),
                req.firstName(), req.lastName(), req.avatarUrl());
        UserSession session = sessionService.createSession(user.getUserId());
        String jwt = jwtService.generate(user);

        JSONObject body = new JSONObject();
        body.put("token", jwt);
        body.put("refreshToken", session.getRefreshToken());
        body.put("userId", user.getUserId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        log.info("OAuth exchange successful: provider={}, userId={}", req.provider(), user.getUserId());
        return ResponseEntity.ok(body.toString());
    }
}
