package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDTO(
        @NotBlank(message = "Token is required") String token
) {}
