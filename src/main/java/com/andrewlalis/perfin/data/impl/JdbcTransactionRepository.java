package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.DbUtil;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public record JdbcTransactionRepository(Connection conn) implements TransactionRepository {
    @Override
    public long insert(Transaction transaction, Map<Long, AccountEntry.Type> accountsMap) {
        final Timestamp timestamp = DbUtil.timestampFromUtcNow();
        return DbUtil.doTransaction(conn, () -> {
            long txId = insertTransaction(timestamp, transaction);
            insertAccountEntriesForTransaction(timestamp, txId, transaction, accountsMap);
            return txId;
        });
    }

    @Override
    public void addAttachments(long transactionId, List<TransactionAttachment> attachments) {
        final Timestamp timestamp = DbUtil.timestampFromUtcNow();
        DbUtil.doTransaction(conn, () -> {
            for (var attachment : attachments) {
                DbUtil.insertOne(
                        conn,
                        "INSERT INTO transaction_attachment (uploaded_at, transaction_id, filename, content_type) VALUES (?, ?, ?, ?)",
                        List.of(
                                timestamp,
                                transactionId,
                                attachment.getFilename(),
                                attachment.getContentType()
                        )
                );
            }
        });
    }

    private long insertTransaction(Timestamp timestamp, Transaction transaction) {
        return DbUtil.insertOne(
                conn,
                "INSERT INTO transaction (timestamp, amount, currency, description) VALUES (?, ?, ?, ?)",
                List.of(
                        timestamp,
                        transaction.getAmount(),
                        transaction.getCurrency().getCurrencyCode(),
                        transaction.getDescription()
                )
        );
    }

    private void insertAccountEntriesForTransaction(
            Timestamp timestamp,
            long txId,
            Transaction transaction,
            Map<Long, AccountEntry.Type> accountsMap
    ) throws SQLException {
        try (var stmt = conn.prepareStatement(
                "INSERT INTO account_entry (timestamp, account_id, transaction_id, amount, type, currency) VALUES (?, ?, ?, ?, ?, ?)"
        )) {
            for (var entry : accountsMap.entrySet()) {
                long accountId = entry.getKey();
                AccountEntry.Type entryType = entry.getValue();
                DbUtil.setArgs(stmt, List.of(
                        timestamp,
                        accountId,
                        txId,
                        transaction.getAmount(),
                        entryType.name(),
                        transaction.getCurrency().getCurrencyCode()
                ));
                stmt.executeUpdate();
            }
        }
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
    public CreditAndDebitAccounts findLinkedAccounts(long transactionId) {
        Account creditAccount = DbUtil.findOne(
                conn,
                """
                        SELECT *
                        FROM account
                        LEFT JOIN account_entry ON account_entry.account_id = account.id
                        WHERE account_entry.transaction_id = ? AND account_entry.type = 'CREDIT'
                        """,
                List.of(transactionId),
                JdbcAccountRepository::parseAccount
        ).orElse(null);
        Account debitAccount = DbUtil.findOne(
                conn,
                """
                        SELECT *
                        FROM account
                        LEFT JOIN account_entry ON account_entry.account_id = account.id
                        WHERE account_entry.transaction_id = ? AND account_entry.type = 'DEBIT'
                        """,
                List.of(transactionId),
                JdbcAccountRepository::parseAccount
        ).orElse(null);
        return new CreditAndDebitAccounts(creditAccount, debitAccount);
    }

    @Override
    public List<TransactionAttachment> findAttachments(long transactionId) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction_attachment WHERE transaction_id = ? ORDER BY filename ASC",
                List.of(transactionId),
                JdbcTransactionRepository::parseAttachment
        );
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

    public static TransactionAttachment parseAttachment(ResultSet rs) throws SQLException {
        return new TransactionAttachment(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("uploaded_at")),
                rs.getLong("transaction_id"),
                rs.getString("filename"),
                rs.getString("content_type")
        );
    }
}
