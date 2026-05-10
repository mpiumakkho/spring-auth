package com.mp.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.ForgotPasswordRequestDTO;
import com.mp.core.dto.ResetPasswordRequestDTO;
import com.mp.core.dto.VerifyEmailRequestDTO;
import com.mp.core.service.AccountRecoveryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account")
public class AccountRecoveryController {

    private final AccountRecoveryService recoveryService;

    public AccountRecoveryController(AccountRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request) {
        recoveryService.verifyEmail(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        recoveryService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        recoveryService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
