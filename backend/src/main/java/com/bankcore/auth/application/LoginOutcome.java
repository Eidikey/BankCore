package com.bankcore.auth.application;

import java.time.Instant;

public sealed interface LoginOutcome {

    record Success(IssuedTokens tokens) implements LoginOutcome {
    }

    record InvalidCredentials() implements LoginOutcome {
    }

    record Locked(Instant lockedUntil) implements LoginOutcome {
    }
}
