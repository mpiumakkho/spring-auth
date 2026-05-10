package com.mp.core.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDTO(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio
) {}
