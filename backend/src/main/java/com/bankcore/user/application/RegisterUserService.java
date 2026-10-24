package com.bankcore.user.application;

import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.UnprocessableException;
import com.bankcore.user.domain.User;
import com.bankcore.user.infrastructure.UserRepository;
import com.bankcore.user.presentation.dto.RegisterRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegisterUserService {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 120;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public RegisterUserService(UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        validateAge(request.birthDate());

        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.registerClient(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                request.birthDate(),
                request.gender(),
                request.occupation().trim(),
                clock.instant());

        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // The unique index is the final guard against concurrent registrations.
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateAge(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now(clock)).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new UnprocessableException(ErrorCode.INVALID_AGE);
        }
    }
}
