package com.bankcore.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final Duration LOCK = Duration.ofMinutes(15);

    @Test
    void locksAfterThreeFailedAttemptsAndDoesNotExtendDuringLock() {
        User user = newUser();

        user.recordFailedLogin(NOW, LOCK);
        user.recordFailedLogin(NOW, LOCK);
        assertThat(user.getFailedLoginAttempts()).isEqualTo((short) 2);
        assertThat(user.isLocked(NOW)).isFalse();

        user.recordFailedLogin(NOW, LOCK);
        assertThat(user.getFailedLoginAttempts()).isEqualTo((short) 3);
        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.getLockedUntil()).isEqualTo(NOW.plus(LOCK));

        Instant deadline = user.getLockedUntil();
        user.recordFailedLogin(NOW.plusSeconds(60), LOCK);
        assertThat(user.getFailedLoginAttempts()).isEqualTo((short) 3);
        assertThat(user.getLockedUntil()).isEqualTo(deadline);
    }

    @Test
    void clearsExpiredLockAndAllowsFreshAttempts() {
        User user = newUser();
        user.recordFailedLogin(NOW, LOCK);
        user.recordFailedLogin(NOW, LOCK);
        user.recordFailedLogin(NOW, LOCK);

        user.clearExpiredLock(NOW.plus(LOCK));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked(NOW.plus(LOCK))).isFalse();
    }

    @Test
    void successfulLoginResetsFailures() {
        User user = newUser();
        user.recordFailedLogin(NOW, LOCK);
        user.recordFailedLogin(NOW, LOCK);

        user.resetFailedLogins(NOW);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    private User newUser() {
        return User.registerClient(
                UUID.randomUUID(),
                "ana@example.com",
                "$argon2id$hash",
                "Ana",
                "García",
                LocalDate.of(1995, 4, 12),
                Gender.FEMALE,
                "Software engineer",
                NOW);
    }
}
