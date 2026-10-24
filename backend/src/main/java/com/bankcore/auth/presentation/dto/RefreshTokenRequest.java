package com.bankcore.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RefreshTokenRequest")
public record RefreshTokenRequest(
        @NotBlank
        @Size(max = 128)
        @Schema(description = "Opaque refresh token returned by login")
        String refreshToken
) {
}
