package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusFilterRequestDTO(
        @NotBlank(message = "Status is required") String status
) {}
