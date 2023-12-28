package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.BalanceRecordRepository;
import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.model.BalanceRecord;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;

public record JdbcBalanceRecordRepository(Connection conn) implements BalanceRecordRepository {
    @Override
    public long insert(BalanceRecord record) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO balance_record (timestamp, account_id, balance, currency) VALUES (?, ?, ?, ?)",
                List.of(
                        DbUtil.timestampFromUtcNow(),
                        record.getAccountId(),
                        record.getBalance(),
                        record.getCurrency().getCurrencyCode()
                )
        );
    }

    @Override
    public BalanceRecord findLatestByAccountId(long accountId) {
        return DbUtil.findOne(
                conn,
                "SELECT * FROM balance_record WHERE account_id = ? ORDER BY timestamp DESC LIMIT 1",
                List.of(accountId),
                JdbcBalanceRecordRepository::parse
        ).orElse(null);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static BalanceRecord parse(ResultSet rs) throws SQLException {
        return new BalanceRecord(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getLong("account_id"),
                rs.getBigDecimal("balance"),
                Currency.getInstance(rs.getString("currency"))
        );
    }
}
