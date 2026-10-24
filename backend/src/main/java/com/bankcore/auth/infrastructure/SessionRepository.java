package com.bankcore.auth.infrastructure;

import com.bankcore.auth.domain.Session;
import com.bankcore.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenHash(String refreshTokenHash);

    @Query("select s from Session s join fetch s.user where s.refreshTokenHash = :hash")
    Optional<Session> findByRefreshTokenHashWithUser(@Param("hash") String hash);

    @Query("""
            select count(s)
            from Session s
            where s.id = :sessionId
              and s.user.id = :userId
              and s.revokedAt is null
              and s.expiresAt > :now
              and s.user.status = :activeStatus
            """)
    long countActive(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId,
            @Param("now") Instant now,
            @Param("activeStatus") UserStatus activeStatus);
}
