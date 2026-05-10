package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetAttributeRequestDTO(
        @NotBlank @Size(max = 100) String key,
        @Size(max = 500) String value
) {}
