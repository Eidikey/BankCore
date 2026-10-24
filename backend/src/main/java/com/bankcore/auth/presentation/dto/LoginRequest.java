package com.bankcore.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest")
public record LoginRequest(
        @NotBlank @Email @Size(max = 254)
        @Schema(example = "ana@example.com")
        String email,

        @NotBlank @Size(max = 128)
        @Schema(example = "una-clave-segura")
        String password
) {
}
