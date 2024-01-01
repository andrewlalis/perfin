package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Transaction;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransactionRepository extends AutoCloseable {
    long insert(
            LocalDateTime utcTimestamp,
            BigDecimal amount,
            Currency currency,
            String description,
            CreditAndDebitAccounts linkedAccounts,
            List<Path> attachments
    );
    Optional<Transaction> findById(long id);
    Page<Transaction> findAll(PageRequest pagination);
    long countAll();
    long countAllAfter(long transactionId);
    Page<Transaction> findAllByAccounts(Set<Long> accountIds, PageRequest pagination);
    CreditAndDebitAccounts findLinkedAccounts(long transactionId);
    List<Attachment> findAttachments(long transactionId);
    void delete(long transactionId);
}
