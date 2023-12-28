package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.BalanceRecord;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

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
    public Page<Account> findAll(PageRequest pagination) {
        return DbUtil.findAll(conn, "SELECT * FROM account WHERE NOT archived", pagination, JdbcAccountRepository::parseAccount);
    }

    @Override
    public List<Account> findAllByCurrency(Currency currency) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM account WHERE currency = ? AND NOT archived ORDER BY created_at",
                List.of(currency.getCurrencyCode()),
                JdbcAccountRepository::parseAccount
        );
    }

    @Override
    public Optional<Account> findById(long id) {
        return DbUtil.findById(conn, "SELECT * FROM account WHERE id = ?", id, JdbcAccountRepository::parseAccount);
    }

    @Override
    public BigDecimal deriveBalance(long id, Instant timestamp) {
        // Find the most recent balance record before timestamp.
        Optional<BalanceRecord> closestPastRecord = DbUtil.findOne(
                conn,
                "SELECT * FROM balance_record WHERE account_id = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 1",
                List.of(id, DbUtil.timestampFromInstant(timestamp)),
                JdbcBalanceRecordRepository::parse
        );
        if (closestPastRecord.isPresent()) {
            // Then find any entries on the account since that balance record and the timestamp.
            List<AccountEntry> accountEntries = DbUtil.findAll(
                    conn,
                    "SELECT * FROM account_entry WHERE account_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC",
                    List.of(
                            id,
                            DbUtil.timestampFromUtcLDT(closestPastRecord.get().getTimestamp()),
                            DbUtil.timestampFromInstant(timestamp)
                    ),
                    JdbcAccountEntryRepository::parse
            );
            // Apply all entries to the most recent known balance to obtain the balance at this point.
            BigDecimal currentBalance = closestPastRecord.get().getBalance();
            for (var entry : accountEntries) {
                currentBalance = currentBalance.add(entry.getSignedAmount());
            }
            return currentBalance;
        } else {
            // There is no balance record present before the given timestamp. Try and find the closest one after.
            Optional<BalanceRecord> closestFutureRecord = DbUtil.findOne(
                    conn,
                    "SELECT * FROM balance_record WHERE account_id = ? AND timestamp >= ? ORDER BY timestamp ASC LIMIT 1",
                    List.of(id, DbUtil.timestampFromInstant(timestamp)),
                    JdbcBalanceRecordRepository::parse
            );
            if (closestFutureRecord.isEmpty()) {
                throw new IllegalStateException("No balance record exists for account " + id);
            }
            // Now find any entries on the account from the timestamp until that balance record.
            List<AccountEntry> accountEntries = DbUtil.findAll(
                    conn,
                    "SELECT * FROM account_entry WHERE account_id = ? AND timestamp <= ? AND timestamp >= ? ORDER BY timestamp DESC",
                    List.of(
                            id,
                            DbUtil.timestampFromUtcLDT(closestFutureRecord.get().getTimestamp()),
                            DbUtil.timestampFromInstant(timestamp)
                    ),
                    JdbcAccountEntryRepository::parse
            );
            BigDecimal currentBalance = closestFutureRecord.get().getBalance();
            for (var entry : accountEntries) {
                currentBalance = currentBalance.subtract(entry.getSignedAmount());
            }
            return currentBalance;
        }
    }

    @Override
    public Set<Currency> findAllUsedCurrencies() {
        return new HashSet<>(DbUtil.findAll(
                conn,
                "SELECT currency FROM account WHERE NOT archived ORDER BY currency ASC",
                rs -> Currency.getInstance(rs.getString(1))
        ));
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
        DbUtil.updateOne(conn, "DELETE FROM account WHERE id = ?", List.of(account.getId()));
    }

    @Override
    public void archive(Account account) {
        DbUtil.updateOne(conn, "UPDATE account SET archived = TRUE WHERE id = ?", List.of(account.getId()));
    }

    public static Account parseAccount(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        LocalDateTime createdAt = DbUtil.utcLDTFromTimestamp(rs.getTimestamp("created_at"));
        boolean archived = rs.getBoolean("archived");
        AccountType type = AccountType.valueOf(rs.getString("account_type").toUpperCase());
        String accountNumber = rs.getString("account_number");
        String name = rs.getString("name");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        return new Account(id, createdAt, archived, type, accountNumber, name, currency);
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }
}
