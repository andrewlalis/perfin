package com.andrewlalis.perfin.data.search;

import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Currency;

public class JdbcTransactionSearcher extends JdbcEntitySearcher<Transaction> {
    public JdbcTransactionSearcher(Connection conn) {
        super(
                conn,
                "SELECT COUNT(transaction.id) FROM transaction",
                "SELECT transaction.* FROM transaction",
                JdbcTransactionSearcher::parseResultSet
        );
    }

    private static Transaction parseResultSet(ResultSet rs) throws SQLException {
        long id = rs.getLong(1);
        LocalDateTime timestamp = DbUtil.utcLDTFromTimestamp(rs.getTimestamp(2));
        BigDecimal amount = rs.getBigDecimal(3);
        Currency currency = Currency.getInstance(rs.getString(4));
        String description = rs.getString(5);
        Long vendorId = rs.getLong(6);
        if (rs.wasNull()) vendorId = null;
        Long categoryId = rs.getLong(7);
        if (rs.wasNull()) categoryId = null;
        return new Transaction(id, timestamp, amount, currency, description, vendorId, categoryId);
    }
}
