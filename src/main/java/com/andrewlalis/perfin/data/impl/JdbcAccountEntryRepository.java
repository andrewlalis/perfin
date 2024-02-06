package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountEntryRepository;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.AccountEntry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public record JdbcAccountEntryRepository(Connection conn) implements AccountEntryRepository {
    @Override
    public long insert(LocalDateTime timestamp, long accountId, long transactionId, BigDecimal amount, AccountEntry.Type type, Currency currency) {
        return DbUtil.insertOne(
                conn,
                """
                        INSERT INTO account_entry (timestamp, account_id, transaction_id, amount, type, currency)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                List.of(
                        DbUtil.timestampFromUtcLDT(timestamp),
                        accountId,
                        transactionId,
                        amount,
                        type.name(),
                        currency.getCurrencyCode()
                )
        );
    }

    @Override
    public Optional<AccountEntry> findById(long id) {
        return DbUtil.findById(
                conn,
                "SELECT * FROM account_entry WHERE id = ?",
                id,
                JdbcAccountEntryRepository::parse
        );
    }

    @Override
    public List<AccountEntry> findAllByAccountId(long accountId) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM account_entry WHERE account_id = ? ORDER BY timestamp DESC",
                List.of(accountId),
                JdbcAccountEntryRepository::parse
        );
    }

    @Override
    public List<AccountEntry> findAllByAccountIdBetween(long accountId, LocalDateTime utcMin, LocalDateTime utcMax) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM account_entry WHERE account_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC",
                List.of(
                        accountId,
                        DbUtil.timestampFromUtcLDT(utcMin),
                        DbUtil.timestampFromUtcLDT(utcMax)
                ),
                JdbcAccountEntryRepository::parse
        );
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static AccountEntry parse(ResultSet rs) throws SQLException {
        return new AccountEntry(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getLong("account_id"),
                rs.getLong("transaction_id"),
                rs.getBigDecimal("amount"),
                AccountEntry.Type.valueOf(rs.getString("type")),
                Currency.getInstance(rs.getString("currency"))
        );
    }
}
