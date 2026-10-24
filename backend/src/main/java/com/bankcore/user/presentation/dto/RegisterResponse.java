package com.bankcore.user.presentation.dto;

import com.bankcore.user.domain.Gender;
import com.bankcore.user.domain.Role;
import com.bankcore.user.domain.User;
import com.bankcore.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Schema(name = "RegisterResponse", description = "Registered customer, never includes credentials")
public record RegisterResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        LocalDate birthDate,
        int age,
        Gender gender,
        String occupation,
        Role role,
        UserStatus status,
        Instant createdAt
) {
    public static RegisterResponse from(User user, java.time.Clock clock) {
        int age = Period.between(user.getBirthDate(), LocalDate.now(clock)).getYears();
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate(),
                age,
                user.getGender(),
                user.getOccupation(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
