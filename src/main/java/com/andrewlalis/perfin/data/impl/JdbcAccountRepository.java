package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.data.UncheckedSqlException;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public record JdbcAccountRepository(Connection conn) implements AccountRepository {
    @Override
    public long insert(Account account) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO account (created_at, account_type, account_number, name, currency) VALUES (?, ?, ?, ?, ?)",
                List.of(
                        DbUtil.timestampFromUtcNow(),
                        account.getType().name(),
                        account.getAccountNumber(),
                        account.getName(),
                        account.getCurrency().getCurrencyCode()
                )
        );
    }

    @Override
    public List<Account> findAll() {
        return DbUtil.findAll(conn, "SELECT * FROM account ORDER BY created_at", JdbcAccountRepository::parseAccount);
    }

    @Override
    public Optional<Account> findById(long id) {
        return DbUtil.findById(conn, "SELECT * FROM account WHERE id = ?", id, JdbcAccountRepository::parseAccount);
    }

    @Override
    public BigDecimal deriveCurrentBalance(long id) {
        // TODO: Implement this!
        return BigDecimal.valueOf(0, 4);
    }

    @Override
    public void update(Account account) {
        DbUtil.updateOne(
                conn,
                "UPDATE account SET name = ?, account_number = ?, currency = ?, account_type = ? WHERE id = ?",
                List.of(
                        account.getName(),
                        account.getAccountNumber(),
                        account.getCurrency().getCurrencyCode(),
                        account.getType().name(),
                        account.getId()
                )
        );
    }

    @Override
    public void delete(Account account) {
        try (var stmt = conn.prepareStatement("DELETE FROM account WHERE id = ?")) {
            stmt.setLong(1, account.getId());
            int rows = stmt.executeUpdate();
            if (rows != 1) throw new SQLException("Affected " + rows + " rows instead of expected 1.");
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    private static Account parseAccount(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        LocalDateTime createdAt = DbUtil.utcLDTFromTimestamp(rs.getTimestamp("created_at"));
        AccountType type = AccountType.valueOf(rs.getString("account_type").toUpperCase());
        String accountNumber = rs.getString("account_number");
        String name = rs.getString("name");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new Account(id, createdAt, type, accountNumber, name, currency);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }
}
