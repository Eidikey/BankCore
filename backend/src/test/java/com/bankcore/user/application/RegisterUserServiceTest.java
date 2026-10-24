package com.bankcore.user.application;

import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.UnprocessableException;
import com.bankcore.user.domain.Gender;
import com.bankcore.user.domain.Role;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import com.bankcore.user.presentation.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);
    private static final String PASSWORD = "una-clave-segura";

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        service = new RegisterUserService(users, passwordEncoder, CLOCK);
    }

    @Test
    void acceptsExactlyOneHundredTwentyYearsOld() {
        when(users.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$argon2id$hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.register(request("Ana@Example.com", LocalDate.of(1906, 10, 23)));

        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getRole()).isEqualTo(Role.CLIENT);
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1906, 10, 23));
        verify(users).existsByEmailIgnoreCase("ana@example.com");
    }

    @Test
    void rejectsPeopleOlderThanOneHundredTwenty() {
        RegisterRequest request = request("old@example.com", LocalDate.of(1905, 10, 23));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOfSatisfying(UnprocessableException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_AGE));
    }

    @Test
    void rejectsDuplicateEmailBeforeHashingPassword() {
        when(users.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        RegisterRequest request = request("ANA@example.com", LocalDate.of(1995, 4, 12));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    private RegisterRequest request(String email, LocalDate birthDate) {
        return new RegisterRequest(
                email,
                PASSWORD,
                "Ana",
                "García",
                birthDate,
                Gender.FEMALE,
                "Software engineer");
    }
}
