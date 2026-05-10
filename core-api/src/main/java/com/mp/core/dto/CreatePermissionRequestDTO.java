package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequestDTO(
        @NotBlank(message = "Permission name is required")
        @Pattern(
                regexp = "^[a-zA-Z_]+:[a-zA-Z_]+$",
                message = "Permission must be in format 'resource:action' using only letters and underscores"
        )
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}
