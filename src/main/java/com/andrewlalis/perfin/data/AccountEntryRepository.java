package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.AccountEntry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public interface AccountEntryRepository extends Repository, AutoCloseable {
    long insert(
            LocalDateTime timestamp,
            long accountId,
            long transactionId,
            BigDecimal amount,
            AccountEntry.Type type,
            Currency currency
    );
    Optional<AccountEntry> findById(long id);
    List<AccountEntry> findAllByAccountId(long accountId);
    List<AccountEntry> findAllByAccountIdBetween(long accountId, LocalDateTime utcMin, LocalDateTime utcMax);
}
