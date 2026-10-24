package com.bankcore.account.infrastructure;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query("select a from Account a where a.user.id = :userId order by a.createdAt desc")
    List<Account> findAllOwnedBy(@Param("userId") UUID userId);

    @Query("select a from Account a where a.id = :accountId and a.user.id = :userId")
    Optional<Account> findOwnedById(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId);

    /**
     * Locks the owned account while closing it so a concurrent financial
     * operation cannot change the balance between the zero check and update.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :accountId and a.user.id = :userId")
    Optional<Account> findOwnedByIdForUpdate(
            @Param("accountId") UUID accountId,
            @Param("userId") UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    @Query("""
            select count(a)
            from Account a
            where a.user.id = :userId
              and a.status <> :closedStatus
            """)
    long countOpenByUserId(
            @Param("userId") UUID userId,
            @Param("closedStatus") AccountStatus closedStatus);
}
