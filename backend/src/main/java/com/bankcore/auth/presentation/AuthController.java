package com.bankcore.auth.presentation;

import com.bankcore.auth.application.AccessToken;
import com.bankcore.auth.application.LoginOutcome;
import com.bankcore.auth.application.LoginService;
import com.bankcore.auth.application.SessionService;
import com.bankcore.auth.presentation.dto.AccessTokenResponse;
import com.bankcore.auth.presentation.dto.LoginRequest;
import com.bankcore.auth.presentation.dto.LoginResponse;
import com.bankcore.auth.presentation.dto.RefreshTokenRequest;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.UnauthorizedException;
import com.bankcore.user.application.RegisterUserService;
import com.bankcore.user.presentation.dto.RegisterRequest;
import com.bankcore.user.presentation.dto.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Customer registration, login, refresh and logout")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final SessionService sessionService;
    private final Clock clock;

    public AuthController(
            RegisterUserService registerUserService,
            LoginService loginService,
            SessionService sessionService,
            Clock clock) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer", description = "Creates a CLIENT user with personal data and an Argon2 password hash")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return RegisterResponse.from(registerUserService.register(request), clock);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate", description = "Returns a short-lived JWT and an opaque refresh token")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        noStore(httpResponse);
        LoginOutcome outcome = loginService.login(
                request.email(),
                request.password(),
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest));

        return switch (outcome) {
            case LoginOutcome.Success success -> LoginResponse.from(success.tokens());
            case LoginOutcome.InvalidCredentials ignored ->
                    throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
            case LoginOutcome.Locked ignored ->
                    throw new UnauthorizedException(ErrorCode.USER_TEMPORARILY_LOCKED);
        };
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an access token", description = "Validates an active session and issues a new access token")
    public AccessTokenResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletResponse httpResponse) {
        noStore(httpResponse);
        AccessToken token = sessionService.refresh(request.refreshToken());
        return AccessTokenResponse.from(token);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Revokes the session identified by the refresh token")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        sessionService.logout(request.refreshToken());
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    private InetAddress clientIp(HttpServletRequest request) {
        try {
            return InetAddress.getByName(request.getRemoteAddr());
        } catch (UnknownHostException ex) {
            return null;
        }
    }
}
