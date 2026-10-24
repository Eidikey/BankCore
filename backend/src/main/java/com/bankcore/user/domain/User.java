package com.bankcore.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * BankCore user (TECHNICAL-DESIGN #13, DATABASE-DESIGN #4).
 *
 * <p>Personal data is stored in {@code users} because a user is a person in
 * this MVP. The age is derived from {@code birthDate}; storing an age would go
 * stale immediately.
 */
@Entity
@Table(name = "users")
public class User {

    public static final short MAX_FAILED_LOGIN_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_login_attempts", nullable = false)
    private short failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "occupation", nullable = false, length = 100)
    private String occupation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    private User(
            UUID id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String occupation,
            Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.CLIENT;
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.occupation = occupation;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static User registerClient(
            UUID id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String occupation,
            Instant now) {
        return new User(id, email, passwordHash, firstName, lastName, birthDate, gender, occupation, now);
    }

    /**
     * Registers a failed password attempt. While the account is locked the
     * counter and lock timestamp are left untouched (TECHNICAL-DESIGN #19).
     */
    public void recordFailedLogin(Instant now, Duration lockDuration) {
        if (isLocked(now)) {
            return;
        }
        if (failedLoginAttempts < MAX_FAILED_LOGIN_ATTEMPTS) {
            failedLoginAttempts = (short) (failedLoginAttempts + 1);
        }
        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            lockedUntil = now.plus(lockDuration);
        }
        updatedAt = now;
    }

    /**
     * Clears a lock whose deadline has passed so the user gets a fresh set of
     * attempts. Must be called before evaluating a new password attempt.
     */
    public void clearExpiredLock(Instant now) {
        if (lockedUntil != null && !lockedUntil.isAfter(now)) {
            failedLoginAttempts = 0;
            lockedUntil = null;
            updatedAt = now;
        }
    }

    public void resetFailedLogins(Instant now) {
        if (failedLoginAttempts != 0 || lockedUntil != null) {
            failedLoginAttempts = 0;
            lockedUntil = null;
            updatedAt = now;
        }
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public short getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public String getOccupation() {
        return occupation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
