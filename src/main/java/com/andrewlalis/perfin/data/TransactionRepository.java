package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TransactionRepository extends AutoCloseable {
    long insert(Transaction transaction, Map<Long, AccountEntry.Type> accountsMap);
    void addAttachments(long transactionId, List<TransactionAttachment> attachments);
    Page<Transaction> findAll(PageRequest pagination);
    long countAll();
    Page<Transaction> findAllByAccounts(Set<Long> accountIds, PageRequest pagination);
    Map<AccountEntry, Account> findEntriesWithAccounts(long transactionId);
    CreditAndDebitAccounts findLinkedAccounts(long transactionId);
    List<TransactionAttachment> findAttachments(long transactionId);
    void delete(long transactionId);
}
