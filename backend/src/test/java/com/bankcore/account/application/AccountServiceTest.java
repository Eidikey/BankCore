package com.bankcore.account.application;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.account.infrastructure.AccountRepository;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.ResourceNotFoundException;
import com.bankcore.user.domain.Gender;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private AccountRepository accounts;

    @Mock
    private UserRepository users;

    @Mock
    private AccountNumberGenerator accountNumbers;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accounts, users, accountNumbers, CLOCK);
    }

    @Test
    void createsActiveCurrentAccountWithZeroBalance() {
        User user = user();
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(accounts.countOpenByUserId(USER_ID, AccountStatus.CERRADA)).thenReturn(2L);
        when(accountNumbers.generateUnique()).thenReturn("1028473916");
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = service.create(USER_ID);

        assertThat(account.getAccountNumber()).isEqualTo("1028473916");
        assertThat(account.getAccountType()).isEqualTo(AccountType.CUENTA_CORRIENTE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVA);
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getUser()).isSameAs(user);
    }

    @Test
    void rejectsSixthOpenAccount() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
        when(accounts.countOpenByUserId(USER_ID, AccountStatus.CERRADA)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(USER_ID))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCOUNT_LIMIT_EXCEEDED));
    }

    @Test
    void rejectsUnknownOwner() {
        when(users.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(USER_ID))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void hidesAccountsOwnedByAnotherCustomer() {
        UUID accountId = UUID.randomUUID();
        when(accounts.findOwnedById(accountId, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwn(USER_ID, accountId))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND));
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
