package com.andrewlalis.perfin.model;

import java.time.LocalDateTime;
import java.util.Currency;

/**
 * The representation of a physical account of some sort (checking, savings,
 * credit-card, etc.).
 */
public class Account extends IdEntity {
    private final LocalDateTime createdAt;
    private final boolean archived;

    private AccountType type;
    private String accountNumber;
    private String name;
    private Currency currency;

    public Account(long id, LocalDateTime createdAt, boolean archived, AccountType type, String accountNumber, String name, Currency currency) {
        super(id);
        this.createdAt = createdAt;
        this.archived = archived;
        this.type = type;
        this.accountNumber = accountNumber;
        this.name = name;
        this.currency = currency;
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

    public void setType(AccountType type) {
        this.type = type;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isArchived() {
        return archived;
    }
}
