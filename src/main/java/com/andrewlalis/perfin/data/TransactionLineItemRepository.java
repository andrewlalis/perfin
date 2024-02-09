package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.TransactionLineItem;

import java.util.List;

public interface TransactionLineItemRepository extends Repository, AutoCloseable {
    List<TransactionLineItem> findItems(long transactionId);
    List<TransactionLineItem> saveItems(long transactionId, List<TransactionLineItem> items);
}
