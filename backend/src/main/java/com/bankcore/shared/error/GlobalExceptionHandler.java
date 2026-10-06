package com.bankcore.shared.error;

import com.bankcore.shared.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse build(HttpServletRequest request, int status, ErrorCode code, String message) {
        return ErrorResponse.of(status, code, message, request.getRequestURI());
    }

    private ErrorResponse build(HttpServletRequest request, int status, ErrorCode code) {
        return ErrorResponse.of(status, code, request.getRequestURI());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error: code={}, path={}, message={}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatusValue())
                .body(build(request, ex.getStatusValue(), ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error: path={}, errors={}", request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: path={}, errors={}", request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.INVALID_REQUEST, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request: path={}, error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.INVALID_REQUEST, "Malformed JSON request"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch: path={}, param={}, error={}", request.getRequestURI(), ex.getName(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.INVALID_REQUEST, "Invalid parameter type: " + ex.getName()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing header: path={}, header={}", request.getRequestURI(), ex.getHeaderName());
        if ("Idempotency-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.badRequest()
                    .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.MISSING_IDEMPOTENCY_KEY));
        }
        return ResponseEntity.badRequest()
                .body(build(request, HttpStatus.BAD_REQUEST.value(), ErrorCode.INVALID_REQUEST, "Missing required header: " + ex.getHeaderName()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials: path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(request, HttpStatus.UNAUTHORIZED.value(), ErrorCode.INVALID_CREDENTIALS));
    }

    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication error: path={}, error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(request, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(request, HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_DENIED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(request, HttpStatus.NOT_FOUND.value(), ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found: path={}, error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(request, HttpStatus.NOT_FOUND.value(), ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();
        log.warn("Data integrity violation: path={}, error={}", request.getRequestURI(), message);

        String lower = message != null ? message.toLowerCase() : "";
        if (lower.contains("uk_users_email") || lower.contains("uk_accounts_number") || lower.contains("account_number")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.ACCOUNT_NUMBER_CONFLICT));
        }
        if (lower.contains("uk_user_idempotency") || lower.contains("idempotency")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.DUPLICATE_OPERATION));
        }
        if (lower.contains("ck_accounts_balance") || lower.contains("balance >= 0")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.INSUFFICIENT_BALANCE));
        }
        if (lower.contains("ck_transfers_different") || lower.contains("source_account_id <> destination_account_id")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.SAME_ACCOUNT_TRANSFER));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.CONCURRENCY_CONFLICT));
    }

    @ExceptionHandler({CannotAcquireLockException.class, PessimisticLockException.class, DeadlockLoserDataAccessException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConcurrencyConflict(Exception ex, HttpServletRequest request) {
        log.warn("Concurrency conflict: path={}, error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.CONCURRENCY_CONFLICT));
    }

    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleHibernateConstraintViolation(org.hibernate.exception.ConstraintViolationException ex, HttpServletRequest request) {
        String constraint = ex.getConstraintName();
        log.warn("Hibernate constraint violation: path={}, constraint={}", request.getRequestURI(), constraint);

        if (constraint != null) {
            if (constraint.contains("idempotency")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.DUPLICATE_OPERATION));
            }
            if (constraint.contains("account_number")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.ACCOUNT_NUMBER_CONFLICT));
            }
            if (constraint.contains("balance")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.INSUFFICIENT_BALANCE));
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(request, HttpStatus.CONFLICT.value(), ErrorCode.CONCURRENCY_CONFLICT));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: path={}, error={}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.INTERNAL_ERROR));
    }
}