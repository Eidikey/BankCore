package com.bankcore.account.domain;

import com.bankcore.user.domain.Gender;
import com.bankcore.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @Test
    void opensActiveCurrentAccountWithZeroBalance() {
        Account account = Account.open(UUID.randomUUID(), user(), "1028473916", NOW);

        assertThat(account.getAccountType()).isEqualTo(AccountType.CUENTA_CORRIENTE);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVA);
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getCreatedAt()).isEqualTo(NOW);
        assertThat(account.getClosedAt()).isNull();
    }

    @Test
    void rejectsAccountNumberWithoutExactlyTenDigits() {
        assertThatThrownBy(() -> Account.open(UUID.randomUUID(), user(), "10284739", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private User user() {
        return User.registerClient(
                UUID.randomUUID(),
                "ana@example.com",
                "$argon2id$hash",
                "Ana",
                "García",
                LocalDate.of(1995, 4, 12),
                Gender.FEMALE,
                "Software engineer",
                NOW);
    }
}
