package com.bankcore.auth.application;

import com.bankcore.audit.application.AuditService;
import com.bankcore.audit.domain.AuditEventType;
import com.bankcore.auth.domain.Session;
import com.bankcore.auth.infrastructure.SessionRepository;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.UnauthorizedException;
import com.bankcore.shared.security.JwtProperties;
import com.bankcore.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionService {

    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final SessionRepository sessions;
    private final RefreshTokenHasher refreshTokenHasher;
    private final TokenService tokenService;
    private final JwtProperties properties;
    private final AuditService audit;
    private final Clock clock;

    public SessionService(
            SessionRepository sessions,
            RefreshTokenHasher refreshTokenHasher,
            TokenService tokenService,
            JwtProperties properties,
            AuditService audit,
            Clock clock) {
        this.sessions = sessions;
        this.refreshTokenHasher = refreshTokenHasher;
        this.tokenService = tokenService;
        this.properties = properties;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens createSession(User user, String userAgent, InetAddress ipAddress) {
        Instant now = Instant.now(clock);
        String rawRefreshToken = UUID.randomUUID().toString();
        Session session = Session.create(
                UUID.randomUUID(),
                user,
                refreshTokenHasher.hash(rawRefreshToken),
                now,
                now.plus(properties.refreshTokenTtl()),
                sanitizeUserAgent(userAgent),
                ipAddress);
        sessions.save(session);

        String accessToken = tokenService.issueAccessToken(user, session);
        return new IssuedTokens(
                accessToken,
                "Bearer",
                tokenService.accessTokenTtlSeconds(),
                rawRefreshToken,
                session.getExpiresAt());
    }

    @Transactional
    public AccessToken refresh(String rawRefreshToken) {
        Session session = sessions.findByRefreshTokenHashWithUserForUpdate(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_REFRESH_TOKEN));

        Instant now = Instant.now(clock);
        if (session.isRevoked() || session.isExpired(now) || !session.getUser().isActive()) {
            throw new UnauthorizedException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        session.markUsed(now);
        sessions.save(session);
        String accessToken = tokenService.issueAccessToken(session.getUser(), session);
        return new AccessToken(accessToken, "Bearer", tokenService.accessTokenTtlSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken, String userAgent, InetAddress ipAddress) {
        sessions.findByRefreshTokenHashForUpdate(refreshTokenHasher.hash(rawRefreshToken))
                .ifPresent(session -> {
                    if (!session.isRevoked()) {
                        session.revoke(Instant.now(clock));
                        sessions.save(session);
                        audit.recordAuth(
                                AuditEventType.AUTH_LOGOUT,
                                session.getUser(),
                                Map.of("sessionId", session.getId().toString()),
                                ipAddress,
                                userAgent);
                    }
                });
    }

    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.length() <= MAX_USER_AGENT_LENGTH
                ? userAgent
                : userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
