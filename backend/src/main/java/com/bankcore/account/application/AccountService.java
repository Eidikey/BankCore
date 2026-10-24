package com.bankcore.account.application;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.infrastructure.AccountRepository;
import com.bankcore.audit.application.AuditService;
import com.bankcore.audit.domain.AuditEventType;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.ResourceNotFoundException;
import com.bankcore.shared.exception.UnprocessableException;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    static final int MAX_OPEN_ACCOUNTS = 5;

    private final AccountRepository accounts;
    private final UserRepository users;
    private final AccountNumberGenerator accountNumbers;
    private final AuditService audit;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts,
            UserRepository users,
            AccountNumberGenerator accountNumbers,
            AuditService audit,
            Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.accountNumbers = accountNumbers;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Account create(UUID userId) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        long openAccounts = accounts.countOpenByUserId(userId, AccountStatus.CERRADA);
        if (openAccounts >= MAX_OPEN_ACCOUNTS) {
            throw new ConflictException(ErrorCode.ACCOUNT_LIMIT_EXCEEDED);
        }

        Instant now = Instant.now(clock);
        Account account = Account.open(
                UUID.randomUUID(),
                user,
                accountNumbers.generateUnique(),
                now);
        Account saved = accounts.saveAndFlush(account);
        audit.recordAccount(
                AuditEventType.ACCOUNT_CREATED,
                user,
                saved.getId(),
                Map.of("accountNumber", saved.getAccountNumber()));
        return saved;
    }

    @Transactional
    public Account close(UUID userId, UUID accountId) {
        Account account = accounts.findOwnedByIdForUpdate(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.isClosed()) {
            throw new ConflictException(ErrorCode.ACCOUNT_CLOSED);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new UnprocessableException(ErrorCode.ACCOUNT_CLOSE_REQUIRES_ZERO_BALANCE);
        }

        account.close(Instant.now(clock));
        Account saved = accounts.saveAndFlush(account);
        audit.recordAccount(
                AuditEventType.ACCOUNT_CLOSED,
                saved.getUser(),
                saved.getId(),
                Map.of("accountNumber", saved.getAccountNumber()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Account> listOwn(UUID userId) {
        return accounts.findAllOwnedBy(userId);
    }

    @Transactional(readOnly = true)
    public Account getOwn(UUID userId, UUID accountId) {
        return accounts.findOwnedById(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
