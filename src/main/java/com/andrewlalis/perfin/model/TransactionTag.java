package com.andrewlalis.perfin.model;

/**
 * A tag that can be applied to a transaction to add some user-defined semantic
 * meaning to it.
 */
public class TransactionTag extends IdEntity {
    public static final int NAME_MAX_LENGTH = 63;
    private final String name;

    public TransactionTag(long id, String name) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
