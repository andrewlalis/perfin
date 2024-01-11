package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.BalanceRecord;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public interface BalanceRecordRepository extends AutoCloseable {
    long insert(LocalDateTime utcTimestamp, long accountId, BigDecimal balance, Currency currency, List<Path> attachments);
    BalanceRecord findLatestByAccountId(long accountId);
    Optional<BalanceRecord> findClosestBefore(long accountId, LocalDateTime utcTimestamp);
    Optional<BalanceRecord> findClosestAfter(long accountId, LocalDateTime utcTimestamp);
    List<Attachment> findAttachments(long recordId);
    void deleteById(long id);
}
