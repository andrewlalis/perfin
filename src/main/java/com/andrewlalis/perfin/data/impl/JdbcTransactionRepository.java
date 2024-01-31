package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountEntryRepository;
import com.andrewlalis.perfin.data.AttachmentRepository;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.UncheckedSqlException;
import com.andrewlalis.perfin.model.*;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.*;
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
            String vendor,
            String category,
            Set<String> tags,
            List<Path> attachments
    ) {
        return DbUtil.doTransaction(conn, () -> {
            Long vendorId = null;
            if (vendor != null && !vendor.isBlank()) {
                vendorId = getOrCreateVendorId(vendor.strip());
            }
            Long categoryId = null;
            if (category != null && !category.isBlank()) {
                categoryId = getOrCreateCategoryId(category.strip());
            }
            // Insert the transaction, using a custom JDBC statement to deal with nullables.
            long txId;
            try (var stmt = conn.prepareStatement(
                    "INSERT INTO transaction (timestamp, amount, currency, description, vendor_id, category_id) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                stmt.setTimestamp(1, DbUtil.timestampFromUtcLDT(utcTimestamp));
                stmt.setBigDecimal(2, amount);
                stmt.setString(3, currency.getCurrencyCode());
                if (description != null && !description.isBlank()) {
                    stmt.setString(4, description.strip());
                } else {
                    stmt.setNull(4, Types.VARCHAR);
                }
                if (vendorId != null) {
                    stmt.setLong(5, vendorId);
                } else {
                    stmt.setNull(5, Types.BIGINT);
                }
                if (categoryId != null) {
                    stmt.setLong(6, categoryId);
                } else {
                    stmt.setNull(6, Types.BIGINT);
                }
                int result = stmt.executeUpdate();
                if (result != 1) throw new UncheckedSqlException("Transaction insert returned " + result);
                var rs = stmt.getGeneratedKeys();
                if (!rs.next()) throw new UncheckedSqlException("Transaction insert didn't generate any keys.");
                txId = rs.getLong(1);
            }
            // Insert linked account entries.
            AccountEntryRepository accountEntryRepository = new JdbcAccountEntryRepository(conn);
            linkedAccounts.ifDebit(acc -> accountEntryRepository.insert(utcTimestamp, acc.id, txId, amount, AccountEntry.Type.DEBIT, currency));
            linkedAccounts.ifCredit(acc -> accountEntryRepository.insert(utcTimestamp, acc.id, txId, amount, AccountEntry.Type.CREDIT, currency));
            // Add attachments.
            AttachmentRepository attachmentRepo = new JdbcAttachmentRepository(conn, contentDir);
            for (Path attachmentPath : attachments) {
                Attachment attachment = attachmentRepo.insert(attachmentPath);
                insertAttachmentLink(txId, attachment.id);
            }
            // Add tags.
            for (String tag : tags) {
                try (var stmt = conn.prepareStatement("INSERT INTO transaction_tag_join (transaction_id, tag_id) VALUES (?, ?)")) {
                    long tagId = getOrCreateTagId(tag.toLowerCase().strip());
                    stmt.setLong(1, txId);
                    stmt.setLong(2, tagId);
                    stmt.executeUpdate();
                }

            }
            return txId;
        });
    }

    private long getOrCreateVendorId(String name) {
        var repo = new JdbcTransactionVendorRepository(conn);
        TransactionVendor vendor = repo.findByName(name).orElse(null);
        if (vendor != null) {
            return vendor.id;
        }
        return repo.insert(name);
    }

    private long getOrCreateCategoryId(String name) {
        var repo = new JdbcTransactionCategoryRepository(conn);
        TransactionCategory category = repo.findByName(name).orElse(null);
        if (category != null) {
            return category.id;
        }
        return repo.insert(name, Color.WHITE);
    }

    private long getOrCreateTagId(String name) {
        Optional<Long> optionalId = DbUtil.findOne(
                conn,
                "SELECT id FROM transaction_tag WHERE name = ?",
                List.of(name),
                rs -> rs.getLong(1)
        );
        return optionalId.orElseGet(() ->
                DbUtil.insertOne(conn, "INSERT INTO transaction_tag (name) VALUES (?)", List.of(name))
        );
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
    public List<String> findTags(long transactionId) {
        return DbUtil.findAll(
                conn,
                """
                        SELECT tt.name
                        FROM transaction_tag tt
                        LEFT JOIN transaction_tag_join ttj ON ttj.tag_id = tt.id
                        WHERE ttj.transaction_id = ?
                        ORDER BY tt.name ASC""",
                List.of(transactionId),
                rs -> rs.getString(1)
        );
    }

    @Override
    public List<String> findAllTags() {
        return DbUtil.findAll(
                conn,
                "SELECT name FROM transaction_tag ORDER BY name ASC",
                rs -> rs.getString(1)
        );
    }

    @Override
    public void deleteTag(String name) {
        DbUtil.update(
                conn,
                "DELETE FROM transaction_tag WHERE name = ?",
                name
        );
    }

    @Override
    public long countTagUsages(String name) {
        return DbUtil.count(
                conn,
                """
                    SELECT COUNT(transaction_id)
                    FROM transaction_tag_join
                    WHERE tag_id = (SELECT id FROM transaction_tag WHERE name = ?)""",
                name
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
            String vendor,
            String category,
            Set<String> tags,
            List<Attachment> existingAttachments,
            List<Path> newAttachmentPaths
    ) {
        DbUtil.doTransaction(conn, () -> {
            var entryRepo = new JdbcAccountEntryRepository(conn);
            var attachmentRepo = new JdbcAttachmentRepository(conn, contentDir);
            var vendorRepo = new JdbcTransactionVendorRepository(conn);
            var categoryRepo = new JdbcTransactionCategoryRepository(conn);

            Transaction tx = findById(id).orElseThrow();
            CreditAndDebitAccounts currentLinkedAccounts = findLinkedAccounts(id);
            TransactionVendor currentVendor = tx.getVendorId() == null ? null : vendorRepo.findById(tx.getVendorId()).orElseThrow();
            String currentVendorName = currentVendor == null ? null : currentVendor.getName();
            TransactionCategory currentCategory = tx.getCategoryId() == null ? null : categoryRepo.findById(tx.getCategoryId()).orElseThrow();
            String currentCategoryName = currentCategory == null ? null : currentCategory.getName();
            Set<String> currentTags = new HashSet<>(findTags(id));
            List<Attachment> currentAttachments = findAttachments(id);

            List<String> updateMessages = new ArrayList<>();
            if (!tx.getTimestamp().equals(utcTimestamp)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET timestamp = ? WHERE id = ?", DbUtil.timestampFromUtcLDT(utcTimestamp), id);
                updateMessages.add("Updated timestamp to UTC " + DateUtil.DEFAULT_DATETIME_FORMAT.format(utcTimestamp) + ".");
            }
            BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_UP);
            if (!tx.getAmount().equals(scaledAmount)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET amount = ? WHERE id = ?", scaledAmount, id);
                updateMessages.add("Updated amount to " + CurrencyUtil.formatMoney(new MoneyValue(scaledAmount, currency)) + ".");
            }
            if (!tx.getCurrency().equals(currency)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET currency = ? WHERE id = ?", currency.getCurrencyCode(), id);
                updateMessages.add("Updated currency to " + currency.getCurrencyCode() + ".");
            }
            if (!Objects.equals(tx.getDescription(), description)) {
                DbUtil.updateOne(conn, "UPDATE transaction SET description = ? WHERE id = ?", description, id);
                updateMessages.add("Updated description.");
            }
            boolean shouldUpdateAccountEntries = !tx.getAmount().equals(scaledAmount) ||
                    !tx.getCurrency().equals(currency) ||
                    !tx.getTimestamp().equals(utcTimestamp) ||
                    !currentLinkedAccounts.equals(linkedAccounts);
            if (shouldUpdateAccountEntries) {
                // Delete all entries and re-write them correctly.
                DbUtil.update(conn, "DELETE FROM account_entry WHERE transaction_id = ?", id);
                linkedAccounts.ifCredit(acc -> entryRepo.insert(utcTimestamp, acc.id, id, scaledAmount, AccountEntry.Type.CREDIT, currency));
                linkedAccounts.ifDebit(acc -> entryRepo.insert(utcTimestamp, acc.id, id, scaledAmount, AccountEntry.Type.DEBIT, currency));
                updateMessages.add("Updated linked accounts.");
            }
            // Manage vendor change.
            if (!Objects.equals(vendor, currentVendorName)) {
                if (vendor == null || vendor.isBlank()) {
                    DbUtil.updateOne(conn, "UPDATE transaction SET vendor_id = NULL WHERE id = ?", id);
                } else {
                    long newVendorId = getOrCreateVendorId(vendor);
                    DbUtil.updateOne(conn, "UPDATE transaction SET vendor_id = ? WHERE id = ?", newVendorId, id);
                }
                updateMessages.add("Updated vendor name to \"" + vendor + "\".");
            }
            // Manage category change.
            if (!Objects.equals(category, currentCategoryName)) {
                if (category == null || category.isBlank()) {
                    DbUtil.updateOne(conn, "UPDATE transaction SET category_id = NULL WHERE id = ?", id);
                } else {
                    long newCategoryId = getOrCreateCategoryId(category);
                    DbUtil.updateOne(conn, "UPDATE transaction SET category_id = ? WHERE id = ?", newCategoryId, id);
                }
                updateMessages.add("Updated category name to \"" + category + "\".");
            }
            // Manage tags changes.
            if (!currentTags.equals(tags)) {
                Set<String> tagsAdded = new HashSet<>(tags);
                tagsAdded.removeAll(currentTags);
                Set<String> tagsRemoved = new HashSet<>(currentTags);
                tagsRemoved.removeAll(tags);

                for (var t : tagsRemoved) removeTag(id, t);
                for (var t : tagsAdded) addTag(id, t);

                if (!tagsAdded.isEmpty()) {
                    updateMessages.add("Added tag(s): " + String.join(", ", tagsAdded));
                }
                if (!tagsRemoved.isEmpty()) {
                    updateMessages.add("Removed tag(s): " + String.join(", ", tagsRemoved));
                }
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

            // Add a text history item to any linked accounts detailing the changes.
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

    private void insertAttachmentLink(long transactionId, long attachmentId) {
        DbUtil.insertOne(
                conn,
                "INSERT INTO transaction_attachment (transaction_id, attachment_id) VALUES (?, ?)",
                List.of(transactionId, attachmentId)
        );
    }

    private long getTagId(String name) {
        return DbUtil.findOne(
                conn,
                "SELECT id FROM transaction_tag WHERE name = ?",
                List.of(name),
                rs -> rs.getLong(1)
        ).orElse(-1L);
    }

    private void removeTag(long transactionId, String tag) {
        long id = getTagId(tag);
        if (id != -1) {
            DbUtil.update(conn, "DELETE FROM transaction_tag_join WHERE transaction_id = ? AND tag_id = ?", transactionId, id);
        }
    }

    private void addTag(long transactionId, String tag) {
        long id = getOrCreateTagId(tag);
        boolean exists = DbUtil.count(
                conn,
                "SELECT COUNT(tag_id) FROM transaction_tag_join WHERE transaction_id = ? AND tag_id = ?",
                transactionId,
                id
        ) > 0;
        if (!exists) {
            DbUtil.insertOne(
                    conn,
                    "INSERT INTO transaction_tag_join (transaction_id, tag_id) VALUES (?, ?)",
                    transactionId,
                    id
            );
        }
    }

    public static Transaction parseTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("timestamp")),
                rs.getBigDecimal("amount"),
                Currency.getInstance(rs.getString("currency")),
                rs.getString("description"),
                rs.getObject("vendor_id", Long.class),
                rs.getObject("category_id", Long.class)
        );
    }
}
