package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A single entry depicting a credit or debit to an account, according to the
 * rules of single-entry accounting.
 * <p>
 *     The following rules apply in determining the type of an entry:
 * </p>
 * <ul>
 *     <li>A <em>debit</em> indicates an increase in assets or decrease in liability.</li>
 *     <li>A <em>credit</em> indicates a decrease in assets or increase in liability.</li>
 * </ul>
 * <p>
 *     For example, for a checking account, a debit entry is added when money
 *     is transferred to the account, and credit when money is taken away.
 * </p>
 * <p>
 *     Each entry corresponds to exactly one transaction. For pretty much
 *     everything but personal transfers, one transaction maps to one entry,
 *     but for transferring from one account to another, you'll have one
 *     transaction and an entry for each account.
 * </p>
 * <p>
 *     We don't use double-entry accounting since we're just tracking personal
 *     accounts, so we don't need the granularity of business accounting, and
 *     all those extra accounts would be a burden to casual users.
 * </p>
 */
public class AccountEntry {
    public enum Type {
        CREDIT,
        DEBIT
    }

    private long id;
    private LocalDateTime timestamp;
    private long accountId;
    private long transactionId;
    private BigDecimal amount;
    private Type type;
    private Currency currency;

    public AccountEntry(long id, LocalDateTime timestamp, long accountId, long transactionId, BigDecimal amount, Type type, Currency currency) {
        this.id = id;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
    }

    public AccountEntry(long accountId, long transactionId, BigDecimal amount, Type type, Currency currency) {
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Type getType() {
        return type;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getSignedAmount() {
        return type == Type.DEBIT ? amount : amount.negate();
    }
}
