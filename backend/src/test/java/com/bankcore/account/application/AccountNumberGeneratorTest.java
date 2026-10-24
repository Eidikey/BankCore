package com.bankcore.account.application;

import com.bankcore.account.infrastructure.AccountRepository;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountNumberGeneratorTest {

    @Mock
    private AccountRepository accounts;

    private AccountNumberGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AccountNumberGenerator(accounts);
    }

    @Test
    void generatesBankCoreTenDigitNumber() {
        when(accounts.existsByAccountNumber(anyString())).thenReturn(false);

        String accountNumber = generator.generateUnique();

        assertThat(accountNumber).matches("^10\\d{8}$");
    }

    @Test
    void retriesWhenCandidateAlreadyExists() {
        when(accounts.existsByAccountNumber(anyString())).thenReturn(true, false);

        String accountNumber = generator.generateUnique();

        assertThat(accountNumber).matches("^10\\d{8}$");
        verify(accounts, times(2)).existsByAccountNumber(anyString());
    }

    @Test
    void failsWhenItCannotFindAFreeNumber() {
        when(accounts.existsByAccountNumber(anyString())).thenReturn(true);

        assertThatThrownBy(generator::generateUnique)
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.ACCOUNT_NUMBER_CONFLICT));
    }
}
