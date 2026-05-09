package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record UsernameRequestDTO(
        @NotBlank(message = "Username is required") String username
) {}
