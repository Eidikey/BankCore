package com.bankcore.auth.application;

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
import java.util.Optional;

@Service
public class LoginService {

    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            SessionService sessionService,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
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
            return new LoginOutcome.InvalidCredentials();
        }

        User user = maybeUser.get();
        if (!user.isActive()) {
            // Keep timing close to a real verification without revealing the status.
            passwordEncoder.matches(rawPassword, user.getPasswordHash());
            return new LoginOutcome.InvalidCredentials();
        }

        if (user.isLocked(now)) {
            // The distinct locked code is a product requirement; still pay the
            // hashing cost so a locked account is not trivially distinguishable
            // by response time.
            passwordEncoder.matches(rawPassword, user.getPasswordHash());
            return new LoginOutcome.Locked(user.getLockedUntil());
        }

        user.clearExpiredLock(now);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.recordFailedLogin(now, LOCK_DURATION);
            users.save(user);
            if (user.isLocked(now)) {
                return new LoginOutcome.Locked(user.getLockedUntil());
            }
            return new LoginOutcome.InvalidCredentials();
        }

        user.resetFailedLogins(now);
        users.save(user);
        IssuedTokens tokens = sessionService.createSession(user, userAgent, ipAddress);
        return new LoginOutcome.Success(tokens);
    }
}
