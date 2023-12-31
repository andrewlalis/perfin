package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import com.andrewlalis.perfin.model.history.AccountHistoryItemType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                "INSERT INTO account_history_item_text (item_id, description) VALUES (?, ?)",
                List.of(itemId, text)
        );
    }

    @Override
    public List<AccountHistoryItem> findMostRecentForAccount(long accountId, LocalDateTime utcTimestamp, int count) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM account_history_item WHERE account_id = ? AND timestamp < ? ORDER BY timestamp DESC LIMIT " + count,
                List.of(accountId, DbUtil.timestampFromUtcLDT(utcTimestamp)),
                JdbcAccountHistoryItemRepository::parseHistoryItem
        );
    }

    @Override
    public String getTextItem(long itemId) {
        return DbUtil.findOne(
                conn,
                "SELECT description FROM account_history_item_text WHERE item_id = ?",
                List.of(itemId),
                rs -> rs.getString(1)
        ).orElse(null);
    }

    @Override
    public AccountEntry getAccountEntryItem(long itemId) {
        return DbUtil.findOne(
                conn,
                """
                        SELECT *
                        FROM account_entry
                        LEFT JOIN account_history_item_account_entry h ON h.entry_id = account_entry.id
                        WHERE h.item_id = ?""",
                List.of(itemId),
                JdbcAccountEntryRepository::parse
        ).orElse(null);
    }

    @Override
    public BalanceRecord getBalanceRecordItem(long itemId) {
        return DbUtil.findOne(
                conn,
                """
                        SELECT *
                        FROM balance_record
                        LEFT JOIN account_history_item_balance_record h ON h.record_id = balance_record.id
                        WHERE h.item_id = ?""",
                List.of(itemId),
                JdbcBalanceRecordRepository::parse
        ).orElse(null);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static AccountHistoryItem parseHistoryItem(ResultSet rs) throws SQLException {
        return new AccountHistoryItem(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getLong("account_id"),
                AccountHistoryItemType.valueOf(rs.getString("type"))
        );
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
