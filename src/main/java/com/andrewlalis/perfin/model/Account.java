package com.andrewlalis.perfin.model;

import java.time.LocalDateTime;
import java.util.Currency;

/**
 * The representation of a physical account of some sort (checking, savings,
 * credit-card, etc.).
 */
public class Account {
    private long id;
    private LocalDateTime createdAt;

    private AccountType type;
    private String accountNumber;
    private String name;
    private Currency currency;

    public Account(long id, LocalDateTime createdAt, AccountType type, String accountNumber, String name, Currency currency) {
        this.id = id;
        this.createdAt = createdAt;
        this.type = type;
        this.accountNumber = accountNumber;
        this.name = name;
        this.currency = currency;
    }

    public Account(AccountType type, String accountNumber, String name, Currency currency) {
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

    public long getId() {
        return id;
    }
}
