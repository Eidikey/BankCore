package com.bankcore.user.presentation.dto;

import com.bankcore.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Customer registration request. Personal data mirrors what a retail bank
 * asks for during onboarding. The age is derived from {@code birthDate}.
 */
@Schema(name = "RegisterRequest", description = "Customer registration data")
public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        @Schema(description = "Unique email, case-insensitive", example = "ana@example.com")
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        @Schema(description = "Password, 8 to 128 characters", example = "una-clave-segura")
        String password,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "First name", example = "Ana")
        String firstName,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Last name", example = "García")
        String lastName,

        @NotNull
        @Schema(description = "Date of birth; customer must be between 18 and 120 years old", example = "1995-04-12")
        LocalDate birthDate,

        @NotNull
        @Schema(description = "Gender", example = "FEMALE")
        Gender gender,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Occupation", example = "Software engineer")
        String occupation
) {
}
