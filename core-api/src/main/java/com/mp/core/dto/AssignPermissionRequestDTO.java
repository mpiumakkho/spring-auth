package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignPermissionRequestDTO(
        @NotBlank(message = "Role ID is required") String roleId,
        @NotBlank(message = "Permission ID is required") String permissionId
) {}
