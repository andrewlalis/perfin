package com.andrewlalis.perfin.model;

/**
 * Represents the different possible account types in Perfin.
 */
public enum AccountType {
    CHECKING("Checking"),
    SAVINGS("Savings"),
    CREDIT_CARD("Credit Card");

    private final String name;

    AccountType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static AccountType parse(String s) {
        s = s.strip().toUpperCase();
        return switch (s) {
            case "CHECKING" -> CHECKING;
            case "SAVINGS" -> SAVINGS;
            case "CREDIT CARD", "CREDITCARD" -> CREDIT_CARD;
            default -> throw new IllegalArgumentException("Invalid AccountType string: " + s);
        };
    }
}
