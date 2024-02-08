package com.andrewlalis.perfin.model;

/**
 * Represents the different possible account types in Perfin.
 */
public enum AccountType {
    CHECKING("Checking", true),
    SAVINGS("Savings", true),
    CREDIT_CARD("Credit Card", false),
    BROKERAGE("Brokerage", true);

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
}
