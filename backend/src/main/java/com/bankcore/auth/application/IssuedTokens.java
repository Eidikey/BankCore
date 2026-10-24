package com.bankcore.auth.application;

import java.time.Instant;

public record IssuedTokens(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        Instant refreshExpiresAt) {
}
