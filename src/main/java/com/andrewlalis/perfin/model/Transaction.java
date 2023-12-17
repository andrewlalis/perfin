package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A transaction is a permanent record of a transfer of funds between two
 * accounts.
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
}
