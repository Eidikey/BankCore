package com.bankcore.audit.domain;

import com.bankcore.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit record (SRS RF-039/RF-040, DATABASE-DESIGN #21).
 *
 * <p>The actor may be absent for an unknown login email. Metadata never
 * contains passwords, JWTs or refresh tokens.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private AuditTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "operation_id")
    private UUID operationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    private AuditEvent(
            UUID id,
            User actorUser,
            AuditEventType eventType,
            AuditTargetType targetType,
            UUID targetId,
            UUID operationId,
            Map<String, Object> metadata,
            InetAddress ipAddress,
            String userAgent,
            Instant createdAt) {
        this.id = id;
        this.actorUser = actorUser;
        this.eventType = eventType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.operationId = operationId;
        this.metadata = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }

    public static AuditEvent create(
            UUID id,
            User actorUser,
            AuditEventType eventType,
            AuditTargetType targetType,
            UUID targetId,
            UUID operationId,
            Map<String, Object> metadata,
            InetAddress ipAddress,
            String userAgent,
            Instant createdAt) {
        return new AuditEvent(
                id,
                actorUser,
                eventType,
                targetType,
                targetId,
                operationId,
                metadata,
                ipAddress,
                userAgent,
                createdAt);
    }

    public UUID getId() {
        return id;
    }

    public User getActorUser() {
        return actorUser;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
