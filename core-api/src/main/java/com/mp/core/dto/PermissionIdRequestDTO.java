package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionIdRequestDTO(
        @NotBlank(message = "Permission ID is required") String permissionId
) {}
