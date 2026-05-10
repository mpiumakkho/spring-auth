package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record UserIdRequestDTO(
        @NotBlank(message = "User ID is required") String userId
) {}
