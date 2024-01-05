package com.andrewlalis.perfin.model;

/**
 * Represents the different possible account types in Perfin.
 */
public enum AccountType {
    CHECKING("Checking", true),
    SAVINGS("Savings", true),
    CREDIT_CARD("Credit Card", false);

    private final String name;
    private final boolean debitsPositive;

    AccountType(String name, boolean debitsPositive) {
        this.name = name;
        this.debitsPositive = debitsPositive;
    }

    public boolean areDebitsPositive() {
        return debitsPositive;
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
