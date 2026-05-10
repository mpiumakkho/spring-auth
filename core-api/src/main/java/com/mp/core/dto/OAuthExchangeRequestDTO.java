package com.mp.core.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequestDTO(
        @NotBlank String provider,
        @NotBlank String providerUid,
        String email,
        String firstName,
        String lastName,
        String avatarUrl
) {}
