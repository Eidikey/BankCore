package com.bankcore.auth.application;

public record AccessToken(
        String accessToken,
        String tokenType,
        long expiresIn) {
}
