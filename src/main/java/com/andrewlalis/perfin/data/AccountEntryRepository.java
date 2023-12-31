package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.AccountEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

public interface AccountEntryRepository extends AutoCloseable {
    long insert(
            LocalDateTime timestamp,
            long accountId,
            long transactionId,
            BigDecimal amount,
            AccountEntry.Type type,
            Currency currency
    );
    List<AccountEntry> findAllByAccountId(long accountId);
}
