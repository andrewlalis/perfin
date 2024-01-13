package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountHistoryItemRepository extends Repository, AutoCloseable {
    void recordAccountEntry(LocalDateTime timestamp, long accountId, long entryId);
    void recordBalanceRecord(LocalDateTime timestamp, long accountId, long recordId);
    void recordText(LocalDateTime timestamp, long accountId, String text);
    List<AccountHistoryItem> findMostRecentForAccount(long accountId, LocalDateTime utcTimestamp, int count);
    default Optional<AccountHistoryItem> getMostRecentForAccount(long accountId) {
        var items = findMostRecentForAccount(accountId, DateUtil.nowAsUTC(), 1);
        if (items.isEmpty()) return Optional.empty();
        return Optional.of(items.getFirst());
    }
    String getTextItem(long itemId);
    AccountEntry getAccountEntryItem(long itemId);
    BalanceRecord getBalanceRecordItem(long itemId);
}
