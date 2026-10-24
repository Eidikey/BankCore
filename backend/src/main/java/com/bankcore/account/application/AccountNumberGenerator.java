package com.bankcore.account.application;

import com.bankcore.account.infrastructure.AccountRepository;
import com.bankcore.shared.error.ErrorCode;
import com.bankcore.shared.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Generates the public account number described by SRS RF-010: the BankCore
 * prefix {@code 10} followed by eight random digits, stored as a string.
 *
 * <p>The database unique constraint remains the final guarantee. This
 * component avoids normal collisions by checking before returning a number.
 */
@Component
public class AccountNumberGenerator {

    static final int MAX_ATTEMPTS = 20;
    private static final int RANDOM_BOUND = 100_000_000;
    private static final String BANKCORE_PREFIX = "10";

    private final AccountRepository accounts;
    private final SecureRandom random = new SecureRandom();

    public AccountNumberGenerator(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = BANKCORE_PREFIX
                    + String.format(Locale.ROOT, "%08d", random.nextInt(RANDOM_BOUND));
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException(ErrorCode.ACCOUNT_NUMBER_CONFLICT);
    }
}
