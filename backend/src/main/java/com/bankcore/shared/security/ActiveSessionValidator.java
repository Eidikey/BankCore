package com.bankcore.shared.security;

import com.bankcore.auth.infrastructure.SessionRepository;
import com.bankcore.user.domain.UserStatus;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Rejects access tokens whose session was revoked, expired, or belongs to a
 * disabled user. This makes logout effective before the 15-minute JWT expires.
 */
@Component
public class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {

    private final SessionRepository sessions;
    private final Clock clock;

    public ActiveSessionValidator(SessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sessionId = token.getClaimAsString("sid");
        String userId = token.getSubject();
        if (sessionId == null || userId == null) {
            return failure("Token is missing session claims");
        }
        try {
            long active = sessions.countActive(
                    UUID.fromString(sessionId),
                    UUID.fromString(userId),
                    Instant.now(clock),
                    UserStatus.ACTIVE);
            return active > 0 ? OAuth2TokenValidatorResult.success() : failure("Session is not active");
        } catch (IllegalArgumentException ex) {
            return failure("Token contains invalid identifiers");
        }
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
