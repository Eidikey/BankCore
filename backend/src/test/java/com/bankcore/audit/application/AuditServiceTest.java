package com.bankcore.audit.application;

import com.bankcore.audit.domain.AuditEvent;
import com.bankcore.audit.domain.AuditEventType;
import com.bankcore.audit.domain.AuditTargetType;
import com.bankcore.audit.infrastructure.AuditEventRepository;
import com.bankcore.user.domain.Gender;
import com.bankcore.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AuditEventRepository events;

    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(events, CLOCK);
    }

    @Test
    void recordsAuthEventWithActorTargetAndSanitizedContext() throws Exception {
        User user = user();
        InetAddress ip = InetAddress.getByName("203.0.113.10");
        String userAgent = "a".repeat(600);

        service.recordAuth(
                AuditEventType.AUTH_LOGIN_FAILURE,
                user,
                Map.of("reason", "INVALID_PASSWORD"),
                ip,
                userAgent);

        AuditEvent event = capturedEvent();
        assertThat(event.getEventType()).isEqualTo(AuditEventType.AUTH_LOGIN_FAILURE);
        assertThat(event.getActorUser()).isSameAs(user);
        assertThat(event.getTargetType()).isEqualTo(AuditTargetType.USER);
        assertThat(event.getTargetId()).isEqualTo(USER_ID);
        assertThat(event.getMetadata()).containsEntry("reason", "INVALID_PASSWORD");
        assertThat(event.getIpAddress()).isEqualTo(ip);
        assertThat(event.getUserAgent()).hasSize(512);
        assertThat(event.getCreatedAt()).isEqualTo(Instant.now(CLOCK));
    }

    @Test
    void leavesTargetEmptyWhenUnknownEmailHasNoActor() throws Exception {
        service.recordAuth(
                AuditEventType.AUTH_LOGIN_FAILURE,
                null,
                Map.of("reason", "UNKNOWN_USER"),
                InetAddress.getByName("203.0.113.10"),
                "JUnit");

        AuditEvent event = capturedEvent();
        assertThat(event.getActorUser()).isNull();
        assertThat(event.getTargetType()).isNull();
        assertThat(event.getTargetId()).isNull();
    }

    @Test
    void recordsAccountEventWithoutRequestContext() {
        UUID accountId = UUID.randomUUID();

        service.recordAccount(
                AuditEventType.ACCOUNT_CLOSED,
                user(),
                accountId,
                Map.of("accountNumber", "1028473916"));

        AuditEvent event = capturedEvent();
        assertThat(event.getEventType()).isEqualTo(AuditEventType.ACCOUNT_CLOSED);
        assertThat(event.getTargetType()).isEqualTo(AuditTargetType.ACCOUNT);
        assertThat(event.getTargetId()).isEqualTo(accountId);
        assertThat(event.getIpAddress()).isNull();
        assertThat(event.getUserAgent()).isNull();
        assertThat(event.getMetadata()).containsEntry("accountNumber", "1028473916");
    }

    private AuditEvent capturedEvent() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    private User user() {
        return User.registerClient(
                USER_ID,
                "ana@example.com",
                "$argon2id$hash",
                "Ana",
                "García",
                LocalDate.of(1995, 4, 12),
                Gender.FEMALE,
                "Software engineer",
                Instant.now(CLOCK));
    }
}
