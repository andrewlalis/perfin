package com.andrewlalis.perfin.data;

import java.time.LocalDateTime;

public interface AccountHistoryItemRepository extends AutoCloseable {
    void recordAccountEntry(LocalDateTime timestamp, long accountId, long entryId);
    void recordBalanceRecord(LocalDateTime timestamp, long accountId, long recordId);
    void recordText(LocalDateTime timestamp, long accountId, String text);
}
