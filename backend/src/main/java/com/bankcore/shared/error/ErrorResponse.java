package com.bankcore.shared.error;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ErrorResponse of(int status, ErrorCode code, String path) {
        return new ErrorResponse(Instant.now(), status, code.name(), code.getDefaultMessage(), path);
    }

    public static ErrorResponse of(int status, ErrorCode code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code.name(), message, path);
    }
}