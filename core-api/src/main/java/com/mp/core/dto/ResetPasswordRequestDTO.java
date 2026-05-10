package com.mp.core.dto;

import com.mp.core.validation.PasswordPolicy;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDTO(
        @NotBlank String token,
        @NotBlank @PasswordPolicy String newPassword
) {}
