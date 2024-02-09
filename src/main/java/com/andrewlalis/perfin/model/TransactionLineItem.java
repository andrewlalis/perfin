package com.andrewlalis.perfin.model;

import java.math.BigDecimal;

/**
 * A line item that comprises part of a transaction. Its total value (value per
 * item * quantity) is part of the transaction's total value. It can be used to
 * record some transactions, like purchases and invoices, in more granular
 * detail.
 */
public class TransactionLineItem extends IdEntity {
    public static final int DESCRIPTION_MAX_LENGTH = 255;

    private final long transactionId;
    private final BigDecimal valuePerItem;
    private final int quantity;
    private final int idx;
    private final String description;
    private final Long categoryId;

    public TransactionLineItem(long id, long transactionId, BigDecimal valuePerItem, int quantity, int idx, String description, Long categoryId) {
        super(id);
        this.transactionId = transactionId;
        this.valuePerItem = valuePerItem;
        this.quantity = quantity;
        this.idx = idx;
        this.description = description;
        this.categoryId = categoryId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public BigDecimal getValuePerItem() {
        return valuePerItem;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getIdx() {
        return idx;
    }

    public String getDescription() {
        return description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getTotalValue() {
        return valuePerItem.multiply(new BigDecimal(quantity));
    }

    @Override
    public String toString() {
        return String.format(
                "TransactionLineItem(id=%d, transactionId=%d, valuePerItem=%s, quantity=%d, idx=%d, description=\"%s\")",
                id,
                transactionId,
                valuePerItem.toPlainString(),
                quantity,
                idx,
                description
        );
    }
}
