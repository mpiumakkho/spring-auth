package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record ResourceActionFilterRequestDTO(
        @NotBlank(message = "Resource is required") String resource,
        @NotBlank(message = "Action is required") String action
) {}
