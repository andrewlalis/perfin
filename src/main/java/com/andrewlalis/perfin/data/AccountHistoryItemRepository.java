package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountHistoryItemRepository extends AutoCloseable {
    void recordAccountEntry(LocalDateTime timestamp, long accountId, long entryId);
    void recordBalanceRecord(LocalDateTime timestamp, long accountId, long recordId);
    void recordText(LocalDateTime timestamp, long accountId, String text);
    List<AccountHistoryItem> findMostRecentForAccount(long accountId, LocalDateTime utcTimestamp, int count);
    String getTextItem(long itemId);
    AccountEntry getAccountEntryItem(long itemId);
    BalanceRecord getBalanceRecordItem(long itemId);
}
