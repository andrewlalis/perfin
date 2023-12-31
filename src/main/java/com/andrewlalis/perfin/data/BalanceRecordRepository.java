package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.BalanceRecord;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

public interface BalanceRecordRepository extends AutoCloseable {
    long insert(LocalDateTime utcTimestamp, long accountId, BigDecimal balance, Currency currency, List<Path> attachments);
    BalanceRecord findLatestByAccountId(long accountId);
}
