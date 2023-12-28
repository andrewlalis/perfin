package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.Transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public record JdbcTransactionRepository(Connection conn) implements TransactionRepository {
    @Override
    public long insert(Transaction transaction, Map<Long, AccountEntry.Type> accountsMap) {
        final Timestamp timestamp = DbUtil.timestampFromUtcNow();
        AtomicLong transactionId = new AtomicLong(-1);
        DbUtil.doTransaction(conn, () -> {
            // First insert the transaction itself, then add account entries, referencing this transaction.
            transactionId.set(DbUtil.insertOne(
                    conn,
                    "INSERT INTO transaction (timestamp, amount, currency, description) VALUES (?, ?, ?, ?)",
                    List.of(
                            timestamp,
                            transaction.getAmount(),
                            transaction.getCurrency().getCurrencyCode(),
                            transaction.getDescription()
                    )
            ));
            // Now insert an account entry for each affected account.
            try (var stmt = conn.prepareStatement(
                    "INSERT INTO account_entry (timestamp, account_id, transaction_id, amount, type, currency) VALUES (?, ?, ?, ?, ?, ?)"
            )) {
                for (var entry : accountsMap.entrySet()) {
                    long accountId = entry.getKey();
                    AccountEntry.Type entryType = entry.getValue();
                    DbUtil.setArgs(stmt, List.of(
                            timestamp,
                            accountId,
                            transactionId.get(),
                            transaction.getAmount(),
                            entryType.name(),
                            transaction.getCurrency().getCurrencyCode()
                    ));
                    stmt.executeUpdate();
                }
            }
        });
        return transactionId.get();
    }

    @Override
    public Page<Transaction> findAll(PageRequest pagination) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction",
                pagination,
                JdbcTransactionRepository::parse
        );
    }

    @Override
    public Page<Transaction> findAllByAccounts(Set<Long> accountIds, PageRequest pagination) {
        String idsStr = accountIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = String.format("""
                SELECT *
                FROM transaction
                LEFT JOIN account_entry ON account_entry.transaction_id = transaction.id
                WHERE account_entry.account_id IN (%s)
                """, idsStr);
        return DbUtil.findAll(conn, query, pagination, JdbcTransactionRepository::parse);
    }

    @Override
    public Map<AccountEntry, Account> findEntriesWithAccounts(long transactionId) {
        List<AccountEntry> entries = DbUtil.findAll(
                conn,
                "SELECT * FROM account_entry WHERE transaction_id = ?",
                List.of(transactionId),
                JdbcAccountEntryRepository::parse
        );
        Map<AccountEntry, Account> map = new HashMap<>();
        for (var entry : entries) {
            Account account = DbUtil.findOne(
                    conn,
                    "SELECT * FROM account WHERE id = ?",
                    List.of(entry.getAccountId()),
                    JdbcAccountRepository::parseAccount
            ).orElseThrow();
            map.put(entry, account);
        }
        return map;
    }

    @Override
    public void delete(long transactionId) {
        DbUtil.updateOne(conn, "DELETE FROM transaction WHERE id = ?", List.of(transactionId));
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static Transaction parse(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getBigDecimal("amount"),
                Currency.getInstance(rs.getString("currency")),
                rs.getString("description")
        );
    }
}
