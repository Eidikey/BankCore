package com.bankcore.shared.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "ErrorResponse",
        description = "Standard BankCore error body (TECHNICAL-DESIGN #40). "
                + "`code` is a stable business identifier.",
        example = """
                {"timestamp":"2026-09-22T20:00:00Z","status":409,"code":"INSUFFICIENT_BALANCE",\
                "message":"Insufficient account balance","path":"/api/v1/transfers"}""")
public record ErrorResponse(
        @Schema(description = "UTC instant when the error was produced", example = "2026-09-22T20:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code, mirrors the response status", example = "409")
        int status,

        @Schema(description = "Stable business error code, see ErrorCode", example = "INSUFFICIENT_BALANCE")
        String code,

        @Schema(description = "Human-readable message, safe to display (no internals leaked)")
        String message,

        @Schema(description = "Request path that produced the error", example = "/api/v1/transfers")
        String path
) {
    public static ErrorResponse of(int status, ErrorCode code, String path) {
        return new ErrorResponse(Instant.now(), status, code.name(), code.getDefaultMessage(), path);
    }

    public static ErrorResponse of(int status, ErrorCode code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code.name(), message, path);
    }
}
