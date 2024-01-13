package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountEntryRepository;
import com.andrewlalis.perfin.data.AttachmentRepository;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public record JdbcTransactionRepository(Connection conn, Path contentDir) implements TransactionRepository {
    @Override
    public long insert(
            LocalDateTime utcTimestamp,
            BigDecimal amount,
            Currency currency,
            String description,
            CreditAndDebitAccounts linkedAccounts,
            List<Path> attachments
    ) {
        return DbUtil.doTransaction(conn, () -> {
            // 1. Insert the transaction.
            long txId = DbUtil.insertOne(
                    conn,
                    "INSERT INTO transaction (timestamp, amount, currency, description) VALUES (?, ?, ?, ?)",
                    List.of(DbUtil.timestampFromUtcLDT(utcTimestamp), amount, currency.getCurrencyCode(), description)
            );
            // 2. Insert linked account entries.
            AccountEntryRepository accountEntryRepository = new JdbcAccountEntryRepository(conn);
            linkedAccounts.ifDebit(acc -> accountEntryRepository.insert(utcTimestamp, acc.id, txId, amount, AccountEntry.Type.DEBIT, currency));
            linkedAccounts.ifCredit(acc -> accountEntryRepository.insert(utcTimestamp, acc.id, txId, amount, AccountEntry.Type.CREDIT, currency));
            // 3. Add attachments.
            AttachmentRepository attachmentRepo = new JdbcAttachmentRepository(conn, contentDir);
            for (Path attachmentPath : attachments) {
                Attachment attachment = attachmentRepo.insert(attachmentPath);
                insertAttachmentLink(txId, attachment.id);
            }
            return txId;
        });
    }

    @Override
    public Optional<Transaction> findById(long id) {
        return DbUtil.findById(conn, "SELECT * FROM transaction WHERE id = ?", id, JdbcTransactionRepository::parseTransaction);
    }

    @Override
    public Page<Transaction> findAll(PageRequest pagination) {
        return DbUtil.findAll(
                conn,
                "SELECT * FROM transaction",
                pagination,
                JdbcTransactionRepository::parseTransaction
        );
    }

    @Override
    public long countAll() {
        return DbUtil.findOne(conn, "SELECT COUNT(id) FROM transaction", Collections.emptyList(), rs -> rs.getLong(1)).orElse(0L);
    }

    @Override
    public long countAllAfter(long transactionId) {
        return DbUtil.findOne(
                conn,
                "SELECT COUNT(id) FROM transaction WHERE timestamp > (SELECT timestamp FROM transaction WHERE id = ?)",
                List.of(transactionId),
                rs -> rs.getLong(1)
        ).orElse(0L);
    }

    @Override
    public long countAllByAccounts(Set<Long> accountIds) {
        String idsStr = accountIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = String.format("""
                SELECT COUNT(transaction.id)
                FROM transaction
                LEFT JOIN account_entry ON account_entry.transaction_id = transaction.id
                WHERE account_entry.account_id IN (%s)
                """, idsStr);
        return DbUtil.findOne(conn, query, Collections.emptyList(), rs -> rs.getLong(1)).orElse(0L);
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
        return DbUtil.findAll(conn, query, pagination, JdbcTransactionRepository::parseTransaction);
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
    public List<Attachment> findAttachments(long transactionId) {
        return DbUtil.findAll(
                conn,
                """
                        SELECT *
                        FROM attachment
                        LEFT JOIN transaction_attachment ta ON ta.attachment_id = attachment.id
                        WHERE ta.transaction_id = ?
                        ORDER BY uploaded_at ASC, filename ASC""",
                List.of(transactionId),
                JdbcAttachmentRepository::parseAttachment
        );
    }

    @Override
    public void delete(long transactionId) {
        DbUtil.doTransaction(conn, () -> {
            DbUtil.updateOne(conn, "DELETE FROM transaction WHERE id = ?", List.of(transactionId));
            DbUtil.update(conn, "DELETE FROM account_entry WHERE transaction_id = ?", List.of(transactionId));
        });
        new JdbcAttachmentRepository(conn, contentDir).deleteAllOrphans();
    }

    @Override
    public void update(
            long id,
            LocalDateTime utcTimestamp,
            BigDecimal amount,
            Currency currency,
            String description,
            CreditAndDebitAccounts linkedAccounts,
            List<Attachment> existingAttachments,
            List<Path> newAttachmentPaths
    ) {
        DbUtil.doTransaction(conn, () -> {
            Transaction tx = findById(id).orElseThrow();
            CreditAndDebitAccounts currentLinkedAccounts = findLinkedAccounts(id);
            List<Attachment> currentAttachments = findAttachments(id);
            var entryRepo = new JdbcAccountEntryRepository(conn);
            var attachmentRepo = new JdbcAttachmentRepository(conn, contentDir);
            List<String> updateMessages = new ArrayList<>();
            if (!tx.getTimestamp().equals(utcTimestamp)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET timestamp = ? WHERE id = ?", List.of(DbUtil.timestampFromUtcLDT(utcTimestamp), id));
                updateMessages.add("Updated timestamp to UTC " + DateUtil.DEFAULT_DATETIME_FORMAT.format(utcTimestamp) + ".");
            }
            BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_UP);
            if (!tx.getAmount().equals(scaledAmount)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET amount = ? WHERE id = ?", List.of(scaledAmount, id));
                updateMessages.add("Updated amount to " + CurrencyUtil.formatMoney(new MoneyValue(scaledAmount, currency)) + ".");
            }
            if (!tx.getCurrency().equals(currency)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET currency = ? WHERE id = ?", List.of(currency.getCurrencyCode(), id));
                updateMessages.add("Updated currency to " + currency.getCurrencyCode() + ".");
            }
            if (!Objects.equals(tx.getDescription(), description)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET description = ? WHERE id = ?", List.of(description, id));
                updateMessages.add("Updated description.");
            }
            boolean updateAccountEntries = !tx.getAmount().equals(scaledAmount) ||
                    !tx.getCurrency().equals(currency) ||
                    !tx.getTimestamp().equals(utcTimestamp) ||
                    !currentLinkedAccounts.equals(linkedAccounts);
            if (updateAccountEntries) {
                // Delete all entries and re-write them correctly?
                DbUtil.update(conn, "DELETE FROM account_entry WHERE transaction_id = ?", List.of(id));
                linkedAccounts.ifCredit(acc -> entryRepo.insert(utcTimestamp, acc.id, id, scaledAmount, AccountEntry.Type.CREDIT, currency));
                linkedAccounts.ifDebit(acc -> entryRepo.insert(utcTimestamp, acc.id, id, scaledAmount, AccountEntry.Type.DEBIT, currency));
                updateMessages.add("Updated linked accounts.");
            }
            // Manage attachments changes.
            List<Attachment> removedAttachments = new ArrayList<>(currentAttachments);
            removedAttachments.removeAll(existingAttachments);
            for (Attachment removedAttachment : removedAttachments) {
                attachmentRepo.deleteById(removedAttachment.id);
                updateMessages.add("Removed attachment \"" + removedAttachment.getFilename() + "\".");
            }
            for (Path attachmentPath : newAttachmentPaths) {
                Attachment attachment = attachmentRepo.insert(attachmentPath);
                insertAttachmentLink(tx.id, attachment.id);
                updateMessages.add("Added attachment \"" + attachment.getFilename() + "\".");
            }
            String updateMessageStr = "Transaction #" + tx.id + " was updated:\n" + String.join("\n", updateMessages);
            var historyRepo = new JdbcAccountHistoryItemRepository(conn);
            linkedAccounts.ifCredit(acc -> historyRepo.recordText(DateUtil.nowAsUTC(), acc.id, updateMessageStr));
            linkedAccounts.ifDebit(acc -> historyRepo.recordText(DateUtil.nowAsUTC(), acc.id, updateMessageStr));
        });
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static Transaction parseTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getBigDecimal("amount"),
                Currency.getInstance(rs.getString("currency")),
                rs.getString("description")
        );
    }

    private void insertAttachmentLink(long transactionId, long attachmentId) {
        DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_attachment (transaction_id, attachment_id) VALUES (?, ?)",
                List.of(transactionId, attachmentId)
        );
    }
}
