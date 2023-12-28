package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountEntryRepository;
import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.model.AccountEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;

public record JdbcAccountEntryRepository(Connection conn) implements AccountEntryRepository {
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
