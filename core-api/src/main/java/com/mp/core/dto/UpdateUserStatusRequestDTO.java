package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequestDTO(
        @NotBlank(message = "User ID is required") String userId,
        @NotBlank(message = "Status is required") String status
) {}
