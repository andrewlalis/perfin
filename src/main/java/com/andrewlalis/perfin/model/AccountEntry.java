package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A single entry depicting a credit or debit to an account.
 * <p>
 *     The following rules apply in determining the type of an entry:
 * </p>
 * <ul>
 *     <li>A <em>debit</em> indicates an increase in assets or decrease in liability.</li>
 *     <li>A <em>credit</em> indicates a decrease in assets or increase in liability.</li>
 * </ul>
 */
public class AccountEntry {
    public enum Type {
        CREDIT,
        DEBIT
    }

    private long id;
    private LocalDateTime timestamp;
    private long accountId;
    private BigDecimal amount;
    private Type type;
    private Currency currency;

    public AccountEntry(long id, LocalDateTime timestamp, long accountId, BigDecimal amount, Type type, Currency currency) {
        this.id = id;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
    }

    public AccountEntry(long accountId, BigDecimal amount, Type type, Currency currency) {
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
    }
}
