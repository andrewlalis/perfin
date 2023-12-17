package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A recording of an account's real reported balance at a given point in time,
 * used as a sanity check for ensuring that an account's entries add up to the
 * correct balance.
 */
public class BalanceRecord {
    private long id;
    private LocalDateTime timestamp;

    private long accountId;
    private BigDecimal balance;
    private Currency currency;

    public BalanceRecord(long id, LocalDateTime timestamp, long accountId, BigDecimal balance, Currency currency) {
        this.id = id;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.balance = balance;
        this.currency = currency;
    }
}
