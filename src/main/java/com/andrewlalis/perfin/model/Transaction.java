package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A transaction is a permanent record of a transfer of funds between two
 * accounts. Its amount is always recorded as an absolute value, and its
 * actual positive/negative effect is determined by the associated account
 * entries that apply this transaction's amount to one or more accounts.
 */
public class Transaction {
    private long id;
    private LocalDateTime timestamp;

    private BigDecimal amount;
    private Currency currency;
    private String description;

    public Transaction(long id, LocalDateTime timestamp, BigDecimal amount, Currency currency, String description) {
        this.id = id;
        this.timestamp = timestamp;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public Transaction(LocalDateTime timestamp, BigDecimal amount, Currency currency, String description) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }
}
