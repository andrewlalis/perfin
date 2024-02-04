package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.HistoryRepository;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.history.HistoryItem;
import com.andrewlalis.perfin.model.history.HistoryTextItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public record JdbcHistoryRepository(Connection conn) implements HistoryRepository {
    @Override
    public long getOrCreateHistoryForAccount(long accountId) {
        return getOrCreateHistoryForEntity(accountId, "history_account", "account_id");
    }

    @Override
    public long getOrCreateHistoryForTransaction(long transactionId) {
        return getOrCreateHistoryForEntity(transactionId, "history_transaction", "transaction_id");
    }

    private long getOrCreateHistoryForEntity(long entityId, String joinTableName, String joinColumn) {
        String selectQuery = "SELECT history_id FROM " + joinTableName + " WHERE " + joinColumn + " = ?";
        var optionalHistoryId = DbUtil.findById(conn, selectQuery, entityId, rs -> rs.getLong(1));
        if (optionalHistoryId.isPresent()) return optionalHistoryId.get();
        long historyId = DbUtil.insertOne(conn, "INSERT INTO history () VALUES ()");
        String insertQuery = "INSERT INTO " + joinTableName + " (" + joinColumn + ", history_id) VALUES (?, ?)";
        DbUtil.updateOne(conn, insertQuery, entityId, historyId);
        return historyId;
    }

    @Override
    public void deleteHistoryForAccount(long accountId) {
        deleteHistoryForEntity(accountId, "history_account", "account_id");
    }

    @Override
    public void deleteHistoryForTransaction(long transactionId) {
        deleteHistoryForEntity(transactionId, "history_transaction", "transaction_id");
    }

    private void deleteHistoryForEntity(long entityId, String joinTableName, String joinColumn) {
        String selectQuery = "SELECT history_id FROM " + joinTableName + " WHERE " + joinColumn + " = ?";
        var optionalHistoryId = DbUtil.findById(conn, selectQuery, entityId, rs -> rs.getLong(1));
        if (optionalHistoryId.isPresent()) {
            long historyId = optionalHistoryId.get();
            DbUtil.updateOne(conn, "DELETE FROM history WHERE id = ?", historyId);
        }
    }

    @Override
    public HistoryTextItem addTextItem(long historyId, LocalDateTime utcTimestamp, String description) {
        long itemId = insertHistoryItem(historyId, utcTimestamp, HistoryItem.TYPE_TEXT);
        DbUtil.updateOne(
                conn,
                "INSERT INTO history_item_text (id, description) VALUES (?, ?)",
                itemId,
                description
        );
        return new HistoryTextItem(itemId, historyId, utcTimestamp, description);
    }

    private long insertHistoryItem(long historyId, LocalDateTime timestamp, String type) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO history_item (history_id, timestamp, type) VALUES (?, ?, ?)",
                historyId,
                DbUtil.timestampFromUtcLDT(timestamp),
                type
        );
    }

    @Override
    public Page<HistoryItem> getItems(long historyId, PageRequest pagination) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM history_item WHERE history_id = ?",
                pagination,
                List.of(historyId),
                JdbcHistoryRepository::parseItem
        );
    }

    @Override
    public List<HistoryItem> getNItemsBefore(long historyId, int n, LocalDateTime timestamp) {
        return DbUtil.findAll(
                conn,
                """
                        SELECT *
                        FROM history_item
                        WHERE history_id = ? AND timestamp <= ?
                        ORDER BY timestamp DESC""",
                List.of(historyId, DbUtil.timestampFromUtcLDT(timestamp)),
                JdbcHistoryRepository::parseItem
        );
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static HistoryItem parseItem(ResultSet rs) throws SQLException {
        long id = rs.getLong(1);
        long historyId = rs.getLong(2);
        LocalDateTime timestamp = DbUtil.utcLDTFromTimestamp(rs.getTimestamp(3));
        String type = rs.getString(4);
        if (type.equalsIgnoreCase(HistoryItem.TYPE_TEXT)) {
            String description = DbUtil.findOne(
                    rs.getStatement().getConnection(),
                    "SELECT description FROM history_item_text WHERE id = ?",
                    List.of(id),
                    r -> r.getString(1)
            ).orElseThrow();
            return new HistoryTextItem(id, historyId, timestamp, description);
        }
        throw new SQLException("Unknown history item type: " + type);
    }
}
