package com.bankcore.auth.presentation.dto;

import com.bankcore.auth.application.IssuedTokens;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "LoginResponse")
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        Instant refreshExpiresAt
) {
    public static LoginResponse from(IssuedTokens tokens) {
        return new LoginResponse(
                tokens.accessToken(),
                tokens.tokenType(),
                tokens.expiresIn(),
                tokens.refreshToken(),
                tokens.refreshExpiresAt());
    }
}
