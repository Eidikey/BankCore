package com.bankcore.shared.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl) {

    @AssertTrue(message = "access-token-ttl must be positive")
    public boolean isAccessTokenTtlPositive() {
        return accessTokenTtl != null && !accessTokenTtl.isZero() && !accessTokenTtl.isNegative();
    }

    @AssertTrue(message = "refresh-token-ttl must be positive")
    public boolean isRefreshTokenTtlPositive() {
        return refreshTokenTtl != null && !refreshTokenTtl.isZero() && !refreshTokenTtl.isNegative();
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=***, issuer=" + issuer
                + ", accessTokenTtl=" + accessTokenTtl
                + ", refreshTokenTtl=" + refreshTokenTtl + "]";
    }
}
