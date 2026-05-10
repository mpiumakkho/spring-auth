package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequestDTO(
        @NotBlank(message = "Role ID is required") String roleId,

        @NotBlank(message = "Role name is required")
        @Size(max = 50, message = "Role name must not exceed 50 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}
