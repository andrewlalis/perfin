package com.andrewlalis.perfin.model;

import java.time.LocalDateTime;
import java.util.Currency;

/**
 * The representation of a physical account of some sort (checking, savings,
 * credit-card, etc.).
 */
public class Account extends IdEntity {
    public static final int DESCRIPTION_MAX_LENGTH = 255;

    private final LocalDateTime createdAt;
    private final boolean archived;

    private final AccountType type;
    private final String accountNumber;
    private final String name;
    private final Currency currency;
    private final String description;

    public Account(long id, LocalDateTime createdAt, boolean archived, AccountType type, String accountNumber, String name, Currency currency, String description) {
        super(id);
        this.createdAt = createdAt;
        this.archived = archived;
        this.type = type;
        this.accountNumber = accountNumber;
        this.name = name;
        this.currency = currency;
        this.description = description;
    }

    public AccountType getType() {
        return type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountNumberSuffix() {
        int suffixLength = Math.min(4, accountNumber.length());
        return "..." + accountNumber.substring(accountNumber.length() - suffixLength);
    }

    public String getAccountNumberGrouped(int groupSize, char separator) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (idx < accountNumber.length()) {
            sb.append(accountNumber.charAt(idx++));
            if (idx % groupSize == 0 && idx < accountNumber.length()) sb.append(separator);
        }
        return sb.toString();
    }

    public String getShortName() {
        String numberSuffix = getAccountNumberSuffix();
        return name + " (" + numberSuffix + ")";
    }

    public String getName() {
        return name;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isArchived() {
        return archived;
    }
}
