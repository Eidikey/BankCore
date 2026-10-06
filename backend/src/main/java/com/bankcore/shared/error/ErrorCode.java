package com.bankcore.shared.error;

public enum ErrorCode {
    // 400 - Invalid request syntax / validation
    INVALID_REQUEST("Invalid request"),
    INVALID_AMOUNT("Invalid amount format"),
    MISSING_IDEMPOTENCY_KEY("Idempotency-Key header is required"),

    // 401 - Authentication
    INVALID_CREDENTIALS("Invalid credentials"),
    UNAUTHORIZED("Unauthorized"),
    SESSION_REVOKED("Session has been revoked"),
    USER_TEMPORARILY_LOCKED("User temporarily locked due to failed attempts"),

    // 403 - Authorization
    FORBIDDEN("Access forbidden"),
    ACCESS_DENIED("Access denied to this resource"),

    // 404 - Not found
    ACCOUNT_NOT_FOUND("Account not found"),
    USER_NOT_FOUND("User not found"),
    TRANSFER_NOT_FOUND("Transfer not found"),
    MOVEMENT_NOT_FOUND("Movement not found"),
    RESOURCE_NOT_FOUND("Resource not found"),

    // 409 - Conflict / business rule violation
    INSUFFICIENT_BALANCE("Insufficient account balance"),
    ACCOUNT_BLOCKED("Account is blocked"),
    ACCOUNT_CLOSED("Account is closed"),
    DUPLICATE_OPERATION("Duplicate operation detected"),
    IDEMPOTENCY_KEY_CONFLICT("Idempotency key reused with different payload"),
    CONCURRENCY_CONFLICT("The operation could not be completed due to a concurrent transaction. Please retry."),
    ACCOUNT_LIMIT_EXCEEDED("Maximum number of active accounts reached"),
    ACCOUNT_NUMBER_CONFLICT("Account number already exists"),
    SAME_ACCOUNT_TRANSFER("Source and destination accounts must be different"),

    // 422 - Semantic business rule violation
    ACCOUNT_CLOSE_REQUIRES_ZERO_BALANCE("Account can only be closed with zero balance"),
    AMOUNT_OUT_OF_RANGE("Amount must be between 1.00 and 50,000.00 MXN"),
    INVALID_ACCOUNT_STATUS("Operation not allowed for current account status"),

    // 500 - Internal error
    INTERNAL_ERROR("An unexpected error occurred");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}