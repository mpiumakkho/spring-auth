package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionNameRequestDTO(
        @NotBlank(message = "Permission name is required") String name
) {}
