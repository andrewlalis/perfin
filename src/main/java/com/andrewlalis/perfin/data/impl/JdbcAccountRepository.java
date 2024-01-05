package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.EntityNotFoundException;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
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
    public long insert(AccountType type, String accountNumber, String name, Currency currency) {
        return DbUtil.doTransaction(conn, () -> {
            long accountId = DbUtil.insertOne(
                    conn,
                    "INSERT INTO account (created_at, account_type, account_number, name, currency) VALUES (?, ?, ?, ?, ?)",
                    List.of(
                            DbUtil.timestampFromUtcNow(),
                            type.name(),
                            accountNumber,
                            name,
                            currency.getCurrencyCode()
                    )
            );
            // Insert a history item indicating the creation of the account.
            var historyRepo = new JdbcAccountHistoryItemRepository(conn);
            historyRepo.recordText(DateUtil.nowAsUTC(), accountId, "Account added to your Perfin profile.");
            return accountId;
        });
    }

    @Override
    public Page<Account> findAll(PageRequest pagination) {
        return DbUtil.findAll(conn, "SELECT * FROM account WHERE NOT archived", pagination, JdbcAccountRepository::parseAccount);
    }

    @Override
    public List<Account> findAllOrderedByRecentHistory() {
        return DbUtil.findAll(
                conn,
                """
                SELECT DISTINCT ON (account.id) account.*, ahi.timestamp AS _
                FROM account
                LEFT OUTER JOIN account_history_item ahi ON ahi.account_id = account.id
                ORDER BY ahi.timestamp DESC, account.created_at DESC""",
                JdbcAccountRepository::parseAccount
        );
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
    public void updateName(long id, String name) {
        DbUtil.updateOne(conn, "UPDATE account SET name = ? WHERE id = ?", List.of(name, id));
    }

    @Override
    public BigDecimal deriveBalance(long accountId, Instant timestamp) {
        // First find the account itself, since its properties influence the balance.
        Account account = findById(accountId).orElse(null);
        if (account == null) throw new EntityNotFoundException(Account.class, accountId);
        // Find the most recent balance record before timestamp.
        Optional<BalanceRecord> closestPastRecord = DbUtil.findOne(
                conn,
                "SELECT * FROM balance_record WHERE account_id = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 1",
                List.of(accountId, DbUtil.timestampFromInstant(timestamp)),
                JdbcBalanceRecordRepository::parse
        );
        if (closestPastRecord.isPresent()) {
            // Then find any entries on the account since that balance record and the timestamp.
            List<AccountEntry> entriesAfterRecord = DbUtil.findAll(
                    conn,
                    "SELECT * FROM account_entry WHERE account_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC",
                    List.of(
                            accountId,
                            DbUtil.timestampFromUtcLDT(closestPastRecord.get().getTimestamp()),
                            DbUtil.timestampFromInstant(timestamp)
                    ),
                    JdbcAccountEntryRepository::parse
            );
            return computeBalanceWithEntriesAfter(account, closestPastRecord.get(), entriesAfterRecord);
        } else {
            // There is no balance record present before the given timestamp. Try and find the closest one after.
            Optional<BalanceRecord> closestFutureRecord = DbUtil.findOne(
                    conn,
                    "SELECT * FROM balance_record WHERE account_id = ? AND timestamp >= ? ORDER BY timestamp ASC LIMIT 1",
                    List.of(accountId, DbUtil.timestampFromInstant(timestamp)),
                    JdbcBalanceRecordRepository::parse
            );
            if (closestFutureRecord.isEmpty()) {
                throw new IllegalStateException("No balance record exists for account " + accountId);
            }
            // Now find any entries on the account from the timestamp until that balance record.
            List<AccountEntry> entriesBeforeRecord = DbUtil.findAll(
                    conn,
                    "SELECT * FROM account_entry WHERE account_id = ? AND timestamp <= ? AND timestamp >= ? ORDER BY timestamp DESC",
                    List.of(
                            accountId,
                            DbUtil.timestampFromUtcLDT(closestFutureRecord.get().getTimestamp()),
                            DbUtil.timestampFromInstant(timestamp)
                    ),
                    JdbcAccountEntryRepository::parse
            );
            return computeBalanceWithEntriesBefore(account, closestFutureRecord.get(), entriesBeforeRecord);
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
                        account.id
                )
        );
    }

    @Override
    public void delete(Account account) {
        DbUtil.updateOne(conn, "DELETE FROM account WHERE id = ?", List.of(account.id));
    }

    @Override
    public void archive(long accountId) {
        DbUtil.doTransaction(conn, () -> {
            DbUtil.updateOne(conn, "UPDATE account SET archived = TRUE WHERE id = ?", List.of(accountId));
            new JdbcAccountHistoryItemRepository(conn).recordText(DateUtil.nowAsUTC(), accountId, "Account has been archived.");
        });
    }

    @Override
    public void unarchive(long accountId) {
        DbUtil.doTransaction(conn, () -> {
            DbUtil.updateOne(conn, "UPDATE account SET archived = FALSE WHERE id = ?", List.of(accountId));
            new JdbcAccountHistoryItemRepository(conn).recordText(DateUtil.nowAsUTC(), accountId, "Account has been unarchived.");
        });
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

    private BigDecimal computeBalanceWithEntriesAfter(Account account, BalanceRecord balanceRecord, List<AccountEntry> entriesAfterRecord) {
        BigDecimal balance = balanceRecord.getBalance();
        for (AccountEntry entry : entriesAfterRecord) {
            balance = balance.add(entry.getEffectiveValue(account.getType()));
        }
        return balance;
    }

    private BigDecimal computeBalanceWithEntriesBefore(Account account, BalanceRecord balanceRecord, List<AccountEntry> entriesBeforeRecord) {
        BigDecimal balance = balanceRecord.getBalance();
        for (AccountEntry entry : entriesBeforeRecord) {
            balance = balance.subtract(entry.getEffectiveValue(account.getType()));
        }
        return balance;
    }
}
