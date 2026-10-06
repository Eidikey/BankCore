package com.bankcore.shared;

import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.error.ErrorResponse;
import com.bankcore.shared.exception.BadRequestException;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.ForbiddenException;
import com.bankcore.shared.exception.ResourceNotFoundException;
import com.bankcore.shared.exception.UnauthorizedException;
import com.bankcore.shared.exception.UnprocessableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller to exercise the global error handler and the OpenAPI
 * error-documentation pattern (each operation declares its {@code ErrorResponse}).
 */
@RestController
@RequestMapping("/api/v1/test-errors")
@Validated
@Tag(name = "test-errors", description = "Test-only endpoints (not part of the public API)")
class TestErrorController {

    record TestRequest(@NotBlank String name, @NotNull @Min(1) Integer amount) {}

    @Operation(summary = "Trigger a 400 business error")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/bad-request")
    ResponseEntity<Void> badRequest(@RequestParam(required = false) String param) {
        throw new BadRequestException(ErrorCode.INVALID_REQUEST, "Custom bad request message");
    }

    @Operation(summary = "Trigger a 401 error")
    @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/unauthorized")
    ResponseEntity<Void> unauthorized() {
        throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
    }

    @Operation(summary = "Trigger a 403 error")
    @ApiResponse(responseCode = "403", description = "Forbidden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/forbidden")
    ResponseEntity<Void> forbidden() {
        throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
    }

    @Operation(summary = "Trigger a 404 error")
    @ApiResponse(responseCode = "404", description = "Not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/not-found")
    ResponseEntity<Void> notFound() {
        throw new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account 123 not found");
    }

    @Operation(summary = "Trigger a 409 conflict")
    @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/conflict")
    ResponseEntity<Void> conflict() {
        throw new ConflictException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Operation(summary = "Trigger a 422 semantic error")
    @ApiResponse(responseCode = "422", description = "Unprocessable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/unprocessable")
    ResponseEntity<Void> unprocessable() {
        throw new UnprocessableException(ErrorCode.ACCOUNT_CLOSE_REQUIRES_ZERO_BALANCE);
    }

    @Operation(summary = "Trigger an unexpected 500 error")
    @ApiResponse(responseCode = "500", description = "Internal error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/runtime")
    ResponseEntity<Void> runtime() {
        throw new RuntimeException("Unexpected error");
    }

    @Operation(summary = "Trigger Bean Validation (400 on invalid body)")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/validation")
    ResponseEntity<Void> validation(@Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Require the Idempotency-Key header",
            parameters = @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER,
                    required = true, description = "Unique key per financial operation"))
    @ApiResponse(responseCode = "400", description = "Missing header",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/idempotency")
    ResponseEntity<Void> idempotency(@RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok().build();
    }
}
