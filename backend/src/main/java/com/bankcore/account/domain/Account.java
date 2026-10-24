package com.bankcore.account.domain;

import com.bankcore.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Bank account owned by a customer (SRS RF-007 to RF-014,
 * DATABASE-DESIGN #7). The UUID is the internal identifier; the ten-digit
 * {@code accountNumber} is the public identifier.
 */
@Entity
@Table(name = "accounts")
public class Account {

    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^\\d{10}$");

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Account() {
    }

    private Account(
            UUID id,
            User user,
            String accountNumber,
            AccountType accountType,
            AccountStatus status,
            BigDecimal balance,
            Instant createdAt,
            Instant updatedAt,
            Instant closedAt) {
        this.id = id;
        this.user = user;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.status = status;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
    }

    /**
     * Opens a new current account in ACTIVE state with a zero balance.
     */
    public static Account open(UUID id, User user, String accountNumber, Instant now) {
        if (accountNumber == null || !ACCOUNT_NUMBER.matcher(accountNumber).matches()) {
            throw new IllegalArgumentException("Account number must contain exactly 10 digits");
        }
        return new Account(
                id,
                user,
                accountNumber,
                AccountType.CUENTA_CORRIENTE,
                AccountStatus.ACTIVA,
                BigDecimal.ZERO.setScale(2),
                now,
                now,
                null);
    }

    /**
     * Permanently closes an open account. The service validates ownership and
     * maps the business errors; this method also protects the invariant for
     * callers inside the domain.
     */
    public void close(Instant now) {
        if (status == AccountStatus.CERRADA) {
            throw new IllegalStateException("Account is already closed");
        }
        if (balance == null || balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account can only be closed with zero balance");
        }
        status = AccountStatus.CERRADA;
        closedAt = now;
        updatedAt = now;
    }

    public boolean isClosed() {
        return status == AccountStatus.CERRADA;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
