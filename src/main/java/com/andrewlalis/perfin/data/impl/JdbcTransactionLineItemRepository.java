package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.TransactionLineItemRepository;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.UncheckedSqlException;
import com.andrewlalis.perfin.model.TransactionLineItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

public record JdbcTransactionLineItemRepository(Connection conn) implements TransactionLineItemRepository {
    @Override
    public List<TransactionLineItem> findItems(long transactionId) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_line_item WHERE transaction_id = ? ORDER BY idx ASC",
                List.of(transactionId),
                JdbcTransactionLineItemRepository::parseItem
        );
    }

    @Override
    public List<TransactionLineItem> saveItems(long transactionId, List<TransactionLineItem> items) {
        // First delete all existing line items since it's just easier that way.
        DbUtil.update(conn, "DELETE FROM transaction_line_item WHERE transaction_id = ?", transactionId);
        if (items.isEmpty()) return Collections.emptyList(); // Skip insertion logic if no items are present.
        String query = """
                INSERT INTO transaction_line_item (
                    transaction_id,
                    value_per_item,
                    quantity,
                    idx,
                    description,
                    category_id
                ) VALUES (?, ?, ?, ?, ?, ?)""";
        try (var stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < items.size(); i++) {
                TransactionLineItem item = items.get(i);
                stmt.setLong(1, transactionId);
                stmt.setBigDecimal(2, item.getValuePerItem());
                stmt.setInt(3, item.getQuantity());
                stmt.setInt(4, i);
                stmt.setString(5, item.getDescription());
                if (item.getCategoryId() == null) {
                    stmt.setNull(6, Types.BIGINT);
                } else {
                    stmt.setLong(6, item.getCategoryId());
                }
                int rowCount = stmt.executeUpdate();
                if (rowCount != 1) throw new SQLException("Failed to insert line item.");
            }
            return findItems(transactionId); // Simply re-fetch items afterward. Their properties may have changed.
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static TransactionLineItem parseItem(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long transactionId = rs.getLong("transaction_id");
        BigDecimal valuePerItem = rs.getBigDecimal("value_per_item");
        int quantity = rs.getInt("quantity");
        int idx = rs.getInt("idx");
        String description = rs.getString("description");
        Long categoryId = rs.getLong("category_id");
        if (rs.wasNull()) categoryId = null;
        return new TransactionLineItem(id, transactionId, valuePerItem, quantity, idx, description, categoryId);
    }
}
