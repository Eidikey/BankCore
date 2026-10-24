package com.bankcore.auth.presentation.dto;

import com.bankcore.auth.application.AccessToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessTokenResponse")
public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static AccessTokenResponse from(AccessToken token) {
        return new AccessTokenResponse(token.accessToken(), token.tokenType(), token.expiresIn());
    }
}
