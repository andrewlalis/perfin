package com.andrewlalis.perfin.model;

public enum AccountType {
    CHECKING,
    SAVINGS,
    CREDIT_CARD;

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
