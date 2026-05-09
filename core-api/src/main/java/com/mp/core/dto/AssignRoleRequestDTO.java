package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequestDTO(
        @NotBlank(message = "User ID is required") String userId,
        @NotBlank(message = "Role ID is required") String roleId
) {}
