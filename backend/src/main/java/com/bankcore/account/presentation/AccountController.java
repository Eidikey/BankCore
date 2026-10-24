package com.bankcore.account.presentation;

import com.bankcore.account.application.AccountService;
import com.bankcore.account.presentation.dto.AccountResponse;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.UnauthorizedException;
import com.bankcore.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Customer account creation and queries")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an account",
            description = "Creates an ACTIVE CUENTA_CORRIENTE account with a unique 10-digit number and zero balance")
    public AccountResponse create(@AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.from(accountService.create(currentUserId(jwt)));
    }

    @GetMapping
    @Operation(
            summary = "List own accounts",
            description = "Returns every account owned by the authenticated customer, including closed accounts")
    public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accountService.listOwn(currentUserId(jwt)).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get own account",
            description = "Returns one account only when it belongs to the authenticated customer")
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return AccountResponse.from(accountService.getOwn(currentUserId(jwt), id));
    }

    private UUID currentUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
        }
    }
}
