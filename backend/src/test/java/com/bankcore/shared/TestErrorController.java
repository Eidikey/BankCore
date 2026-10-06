package com.bankcore.shared;

import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.BadRequestException;
import com.bankcore.shared.exception.ConflictException;
import com.bankcore.shared.exception.ForbiddenException;
import com.bankcore.shared.exception.ResourceNotFoundException;
import com.bankcore.shared.exception.UnauthorizedException;
import com.bankcore.shared.exception.UnprocessableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-errors")
@Validated
class TestErrorController {

    record TestRequest(@NotBlank String name, @NotNull @Min(1) Integer amount) {}

    @GetMapping("/bad-request")
    ResponseEntity<Void> badRequest(@RequestParam(required = false) String param) {
        throw new BadRequestException(ErrorCode.INVALID_REQUEST, "Custom bad request message");
    }

    @GetMapping("/unauthorized")
    ResponseEntity<Void> unauthorized() {
        throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
    }

    @GetMapping("/forbidden")
    ResponseEntity<Void> forbidden() {
        throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
    }

    @GetMapping("/not-found")
    ResponseEntity<Void> notFound() {
        throw new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Account 123 not found");
    }

    @GetMapping("/conflict")
    ResponseEntity<Void> conflict() {
        throw new ConflictException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @GetMapping("/unprocessable")
    ResponseEntity<Void> unprocessable() {
        throw new UnprocessableException(ErrorCode.ACCOUNT_CLOSE_REQUIRES_ZERO_BALANCE);
    }

    @GetMapping("/runtime")
    ResponseEntity<Void> runtime() {
        throw new RuntimeException("Unexpected error");
    }

    @PostMapping("/validation")
    ResponseEntity<Void> validation(@Valid @RequestBody TestRequest request) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/idempotency")
    ResponseEntity<Void> idempotency(@RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.ok().build();
    }
}
