package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AttachmentRepository;
import com.andrewlalis.perfin.data.BalanceRecordRepository;
import com.andrewlalis.perfin.data.HistoryRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.MoneyValue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

public record JdbcBalanceRecordRepository(Connection conn, Path contentDir) implements BalanceRecordRepository {
    @Override
    public long insert(LocalDateTime utcTimestamp, long accountId, BigDecimal balance, Currency currency, List<Path> attachments) {
        return DbUtil.doTransaction(conn, () -> {
            long recordId = DbUtil.insertOne(
                    conn,
                    "INSERT INTO balance_record (timestamp, account_id, balance, currency) VALUES (?, ?, ?, ?)",
                    List.of(DbUtil.timestampFromUtcLDT(utcTimestamp), accountId, balance, currency.getCurrencyCode())
            );
            // Insert attachments.
            AttachmentRepository attachmentRepo = new JdbcAttachmentRepository(conn, contentDir);
            try (var stmt = conn.prepareStatement("INSERT INTO balance_record_attachment(balance_record_id, attachment_id) VALUES (?, ?)")) {
                for (var attachmentPath : attachments) {
                    Attachment attachment = attachmentRepo.insert(attachmentPath);
                    DbUtil.setArgs(stmt, recordId, attachment.id);
                    stmt.executeUpdate();
                }
            }
            return recordId;
        });
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
    public Optional<BalanceRecord> findById(long id) {
        return DbUtil.findById(
                conn,
                "SELECT * FROM balance_record WHERE id = ?",
                id,
                JdbcBalanceRecordRepository::parse
        );
    }

    @Override
    public Optional<BalanceRecord> findClosestBefore(long accountId, LocalDateTime utcTimestamp) {
        return DbUtil.findOne(
                conn,
                "SELECT * FROM balance_record WHERE account_id = ? AND timestamp <= ? ORDER BY timestamp DESC LIMIT 1",
                List.of(accountId, DbUtil.timestampFromUtcLDT(utcTimestamp)),
                JdbcBalanceRecordRepository::parse
        );
    }

    @Override
    public Optional<BalanceRecord> findClosestAfter(long accountId, LocalDateTime utcTimestamp) {
        return DbUtil.findOne(
                conn,
                "SELECT * FROM balance_record WHERE account_id = ? AND timestamp >= ? ORDER BY timestamp ASC LIMIT 1",
                List.of(accountId, DbUtil.timestampFromUtcLDT(utcTimestamp)),
                JdbcBalanceRecordRepository::parse
        );
    }

    @Override
    public List<Attachment> findAttachments(long recordId) {
        return DbUtil.findAll(
                conn,
                """
                        SELECT *
                        FROM attachment
                        LEFT JOIN balance_record_attachment ba ON ba.attachment_id = attachment.id
                        WHERE ba.balance_record_id = ?
                        ORDER BY uploaded_at ASC, filename ASC""",
                List.of(recordId),
                JdbcAttachmentRepository::parseAttachment
        );
    }

    @Override
    public void deleteById(long id) {
        DbUtil.updateOne(conn, "DELETE FROM balance_record WHERE id = ?", List.of(id));
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
