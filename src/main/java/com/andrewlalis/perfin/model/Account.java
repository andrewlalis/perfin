package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
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
    private BigDecimal currentBalance;
    private String name;
    private Currency currency;

    public Account(AccountType type, String accountNumber, BigDecimal currentBalance, String name, Currency currency) {
        this.type = type;
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.name = name;
        this.currency = currency;
    }

    public AccountType getType() {
        return type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public String getName() {
        return name;
    }

    public Currency getCurrency() {
        return currency;
    }
}
