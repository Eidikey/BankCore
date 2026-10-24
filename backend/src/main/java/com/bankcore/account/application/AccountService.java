package com.bankcore.account.application;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.infrastructure.AccountRepository;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.ResourceNotFoundException;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    static final int MAX_OPEN_ACCOUNTS = 5;

    private final AccountRepository accounts;
    private final UserRepository users;
    private final AccountNumberGenerator accountNumbers;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts,
            UserRepository users,
            AccountNumberGenerator accountNumbers,
            Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.accountNumbers = accountNumbers;
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
        return accounts.saveAndFlush(account);
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
