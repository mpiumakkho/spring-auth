package com.mp.core.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.TokenRequestDTO;
import com.mp.core.service.UserSessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
@Slf4j
public class SessionController {

    private final UserSessionService sessionService;

    public SessionController(UserSessionService sessionService) {
        this.sessionService = sessionService;
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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRequestDTO request) {
        sessionService.invalidateSession(request.token());
        return ResponseEntity.ok().build();
    }
}
