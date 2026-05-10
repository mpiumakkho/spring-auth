package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleIdRequestDTO(
        @NotBlank(message = "Role ID is required") String roleId
) {}
