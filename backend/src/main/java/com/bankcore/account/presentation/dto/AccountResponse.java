package com.bankcore.account.presentation.dto;

import com.bankcore.account.domain.Account;
import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "AccountResponse", description = "Bank account owned by the authenticated customer")
public record AccountResponse(
        UUID id,
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getClosedAt());
    }
}
