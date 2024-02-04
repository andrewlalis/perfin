package com.andrewlalis.perfin.model;

/**
 * A vendor is a business establishment that can be linked to a transaction, to
 * denote the business that the transaction took place with.
 */
public class TransactionVendor extends IdEntity {
    public static final int NAME_MAX_LENGTH = 255;
    public static final int DESCRIPTION_MAX_LENGTH = 255;

    private final String name;
    private final String description;

    public TransactionVendor(long id, String name, String description) {
        super(id);
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
