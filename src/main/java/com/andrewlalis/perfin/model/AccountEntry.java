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
public class AccountEntry extends IdEntity implements Timestamped {
    public enum Type {
        CREDIT,
        DEBIT
    }

    private final LocalDateTime timestamp;
    private final long accountId;
    private final long transactionId;
    private final BigDecimal amount;
    private final Type type;
    private final Currency currency;

    public AccountEntry(long id, LocalDateTime timestamp, long accountId, long transactionId, BigDecimal amount, Type type, Currency currency) {
        super(id);
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
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

    public MoneyValue getMoneyValue() {
        return new MoneyValue(amount, currency);
    }

    /**
     * Gets the effective value of this entry for the account's type.
     * @param accountType The type of the account.
     * @return The effective value of this entry, either positive or negative.
     */
    public BigDecimal getEffectiveValue(AccountType accountType) {
        if (accountType.areDebitsPositive()) {
            return type == Type.DEBIT ? amount : amount.negate();
        } else {
            return type == Type.DEBIT ? amount.negate() : amount;
        }
    }
}
