package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDTO(
        @NotBlank(message = "User ID is required") String userId,

        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
