package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.data.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * A transaction is a permanent record of a transfer of funds between two
 * accounts. Its amount is always recorded as an absolute value, and its
 * actual positive/negative effect is determined by the associated account
 * entries that apply this transaction's amount to one or more accounts.
 */
public class Transaction extends IdEntity {
    private final LocalDateTime timestamp;
    private final BigDecimal amount;
    private final Currency currency;
    private final String description;
    private final Long vendorId;
    private final Long categoryId;

    public Transaction(long id, LocalDateTime timestamp, BigDecimal amount, Currency currency, String description, Long vendorId, Long categoryId) {
        super(id);
        this.timestamp = timestamp;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.vendorId = vendorId;
        this.categoryId = categoryId;
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

    public Long getVendorId() {
        return vendorId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public MoneyValue getMoneyAmount() {
        return new MoneyValue(amount, currency);
    }

    @Override
    public String toString() {
        return String.format(
                "Transaction (id=%d, timestamp=%s, amount=%s, currency=%s, description=%s)",
                id,
                timestamp.format(DateUtil.DEFAULT_DATETIME_FORMAT),
                amount.toPlainString(),
                currency.getCurrencyCode(),
                description
        );
    }
}
