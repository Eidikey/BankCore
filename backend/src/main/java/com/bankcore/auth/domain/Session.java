package com.bankcore.auth.domain;

import com.bankcore.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent, revocable session (TECHNICAL-DESIGN #17, DATABASE-DESIGN #5-6).
 * Only the SHA-256 hash of the refresh token is stored.
 */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Session() {
    }

    private Session(
            UUID id,
            User user,
            String refreshTokenHash,
            Instant createdAt,
            Instant expiresAt,
            String userAgent,
            InetAddress ipAddress) {
        this.id = id;
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public static Session create(
            UUID id,
            User user,
            String refreshTokenHash,
            Instant createdAt,
            Instant expiresAt,
            String userAgent,
            InetAddress ipAddress) {
        return new Session(id, user, refreshTokenHash, createdAt, expiresAt, userAgent, ipAddress);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public void markUsed(Instant now) {
        lastUsedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
