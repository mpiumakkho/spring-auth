package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record ResourceFilterRequestDTO(
        @NotBlank(message = "Resource is required") String resource
) {}
