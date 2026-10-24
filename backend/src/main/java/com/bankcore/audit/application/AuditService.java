package com.bankcore.audit.application;

import com.bankcore.audit.domain.AuditEvent;
import com.bankcore.audit.domain.AuditEventType;
import com.bankcore.audit.domain.AuditTargetType;
import com.bankcore.audit.infrastructure.AuditEventRepository;
import com.bankcore.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Writes persistent audit events inside the caller's transaction. If the
 * business operation rolls back, its audit record rolls back with it.
 */
@Service
public class AuditService {

    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void recordAuth(
            AuditEventType eventType,
            User actorUser,
            Map<String, Object> metadata,
            InetAddress ipAddress,
            String userAgent) {
        record(
                eventType,
                actorUser,
                actorUser == null ? null : AuditTargetType.USER,
                actorUser == null ? null : actorUser.getId(),
                metadata,
                ipAddress,
                userAgent);
    }

    @Transactional
    public void recordAccount(
            AuditEventType eventType,
            User actorUser,
            UUID accountId,
            Map<String, Object> metadata) {
        record(eventType, actorUser, AuditTargetType.ACCOUNT, accountId, metadata, null, null);
    }

    private void record(
            AuditEventType eventType,
            User actorUser,
            AuditTargetType targetType,
            UUID targetId,
            Map<String, Object> metadata,
            InetAddress ipAddress,
            String userAgent) {
        events.save(AuditEvent.create(
                UUID.randomUUID(),
                actorUser,
                eventType,
                targetType,
                targetId,
                null,
                metadata,
                ipAddress,
                sanitize(userAgent),
                Instant.now(clock)));
    }

    private String sanitize(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.length() <= MAX_USER_AGENT_LENGTH
                ? userAgent
                : userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
