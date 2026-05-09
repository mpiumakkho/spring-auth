package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleNameRequestDTO(
        @NotBlank(message = "Role name is required") String name
) {}
