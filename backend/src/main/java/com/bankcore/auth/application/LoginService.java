package com.bankcore.auth.application;

import com.bankcore.audit.application.AuditService;
import com.bankcore.audit.domain.AuditEventType;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class LoginService {

    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final AuditService audit;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            SessionService sessionService,
            AuditService audit,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-only");
    }

    @Transactional
    public LoginOutcome login(String email, String rawPassword, String userAgent, InetAddress ipAddress) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now(clock);

        Optional<User> maybeUser = users.findByEmailForUpdate(normalizedEmail);
        if (maybeUser.isEmpty()) {
            // Keep the response time close to a real password check.
            passwordEncoder.matches(rawPassword, dummyHash);
            audit.recordAuth(
                    AuditEventType.AUTH_LOGIN_FAILURE,
                    null,
                    Map.of("reason", "UNKNOWN_USER"),
                    ipAddress,
                    userAgent);
            return new LoginOutcome.InvalidCredentials();
        }

        User user = maybeUser.get();
        if (!user.isActive()) {
            // Keep timing close to a real verification without revealing the status.
            passwordEncoder.matches(rawPassword, user.getPasswordHash());
            audit.recordAuth(
                    AuditEventType.AUTH_LOGIN_FAILURE,
                    user,
                    Map.of("reason", "USER_INACTIVE"),
                    ipAddress,
                    userAgent);
            return new LoginOutcome.InvalidCredentials();
        }

        if (user.isLocked(now)) {
            // The distinct locked code is a product requirement; still pay the
            // hashing cost so a locked account is not trivially distinguishable
            // by response time.
            passwordEncoder.matches(rawPassword, user.getPasswordHash());
            audit.recordAuth(
                    AuditEventType.AUTH_LOGIN_FAILURE,
                    user,
                    Map.of("reason", "USER_LOCKED"),
                    ipAddress,
                    userAgent);
            return new LoginOutcome.Locked(user.getLockedUntil());
        }

        user.clearExpiredLock(now);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.recordFailedLogin(now, LOCK_DURATION);
            users.save(user);
            audit.recordAuth(
                    AuditEventType.AUTH_LOGIN_FAILURE,
                    user,
                    Map.of("reason", "INVALID_PASSWORD"),
                    ipAddress,
                    userAgent);
            if (user.isLocked(now)) {
                audit.recordAuth(
                        AuditEventType.AUTH_TEMP_LOCK,
                        user,
                        Map.of("lockedUntil", user.getLockedUntil().toString()),
                        ipAddress,
                        userAgent);
                return new LoginOutcome.Locked(user.getLockedUntil());
            }
            return new LoginOutcome.InvalidCredentials();
        }

        user.resetFailedLogins(now);
        users.save(user);
        IssuedTokens tokens = sessionService.createSession(user, userAgent, ipAddress);
        audit.recordAuth(
                AuditEventType.AUTH_LOGIN_SUCCESS,
                user,
                Map.of(),
                ipAddress,
                userAgent);
        return new LoginOutcome.Success(tokens);
    }
}
