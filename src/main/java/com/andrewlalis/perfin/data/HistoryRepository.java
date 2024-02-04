package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.history.HistoryItem;
import com.andrewlalis.perfin.model.history.HistoryTextItem;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public interface HistoryRepository extends Repository, AutoCloseable {
    long getOrCreateHistoryForAccount(long accountId);
    long getOrCreateHistoryForTransaction(long transactionId);
    void deleteHistoryForAccount(long accountId);
    void deleteHistoryForTransaction(long transactionId);

    HistoryTextItem addTextItem(long historyId, LocalDateTime utcTimestamp, String description);
    default HistoryTextItem addTextItem(long historyId, String description) {
        return addTextItem(historyId, DateUtil.nowAsUTC(), description);
    }
    Page<HistoryItem> getItems(long historyId, PageRequest pagination);
    List<HistoryItem> getNItemsBefore(long historyId, int n, LocalDateTime timestamp);
    default List<HistoryItem> getNItemsBeforeNow(long historyId, int n) {
        return getNItemsBefore(historyId, n, LocalDateTime.now(ZoneOffset.UTC));
    }
}
