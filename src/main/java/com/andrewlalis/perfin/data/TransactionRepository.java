package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.Transaction;

import java.util.Map;
import java.util.Set;

public interface TransactionRepository extends AutoCloseable {
    long insert(Transaction transaction, Map<Long, AccountEntry.Type> accountsMap);
    Page<Transaction> findAll(PageRequest pagination);
    Page<Transaction> findAllByAccounts(Set<Long> accountIds, PageRequest pagination);
    Map<AccountEntry, Account> findEntriesWithAccounts(long transactionId);
    void delete(long transactionId);
}
