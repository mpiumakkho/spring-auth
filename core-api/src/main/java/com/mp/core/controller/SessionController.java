package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.RefreshTokenRequestDTO;
import com.mp.core.dto.TokenRequestDTO;
import com.mp.core.entity.User;
import com.mp.core.entity.UserSession;
import com.mp.core.exception.ResourceNotFoundException;
import com.mp.core.security.JwtService;
import com.mp.core.service.RefreshTokenService;
import com.mp.core.service.UserService;
import com.mp.core.service.UserSessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sessions")
@Slf4j
public class SessionController {

    private final UserSessionService sessionService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final JwtService jwtService;

    public SessionController(
            UserSessionService sessionService,
            RefreshTokenService refreshTokenService,
            UserService userService,
            JwtService jwtService) {
        this.sessionService = sessionService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/validate")
    public ResponseEntity<Void> validateSession(@Valid @RequestBody TokenRequestDTO request) {
        if (sessionService.isSessionValid(request.token())) {
            sessionService.updateActivity(request.token());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<Void> keepAlive(@Valid @RequestBody TokenRequestDTO request) {
        if (sessionService.isSessionValid(request.token())) {
            sessionService.updateActivity(request.token());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        UserSession session = sessionService.refreshSession(request.refreshToken());
        User user = userService.getUserById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", session.getUserId()));
        String jwt = jwtService.generate(user);
        JSONObject body = new JSONObject();
        body.put("token", jwt);
        body.put("refreshToken", session.getRefreshToken());
        return ResponseEntity.ok(body.toString());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRequestDTO request) {
        sessionService.invalidateSession(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revoke-refresh")
    public ResponseEntity<Void> revokeRefresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.ok().build();
    }
}
