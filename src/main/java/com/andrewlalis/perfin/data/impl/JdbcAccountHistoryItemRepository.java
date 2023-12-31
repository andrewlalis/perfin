package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.model.history.AccountHistoryItemType;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public record JdbcAccountHistoryItemRepository(Connection conn) implements AccountHistoryItemRepository {
    @Override
    public void recordAccountEntry(LocalDateTime timestamp, long accountId, long entryId) {
        long itemId = insertHistoryItem(timestamp, accountId, AccountHistoryItemType.ACCOUNT_ENTRY);
        DbUtil.insertOne(
                conn,
                "INSERT INTO account_history_item_account_entry (item_id, entry_id) VALUES (?, ?)",
                List.of(itemId, entryId)
        );
    }

    @Override
    public void recordBalanceRecord(LocalDateTime timestamp, long accountId, long recordId) {
        long itemId = insertHistoryItem(timestamp, accountId, AccountHistoryItemType.BALANCE_RECORD);
        DbUtil.insertOne(
                conn,
                "INSERT INTO account_history_item_balance_record (item_id, record_id) VALUES (?, ?)",
                List.of(itemId, recordId)
        );
    }

    @Override
    public void recordText(LocalDateTime timestamp, long accountId, String text) {
        long itemId = insertHistoryItem(timestamp, accountId, AccountHistoryItemType.TEXT);
        DbUtil.insertOne(
                conn,
                "INSERT INTO account_history_item_account_entry (item_id, description) VALUES (?, ?)",
                List.of(itemId, text)
        );
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    private long insertHistoryItem(LocalDateTime timestamp, long accountId, AccountHistoryItemType type) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO account_history_item (timestamp, account_id, type) VALUES (?, ?, ?)",
                List.of(
                        DbUtil.timestampFromUtcLDT(timestamp),
                        accountId,
                        type.name()
                )
        );
    }
}
