package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AttachmentRepository;
import com.andrewlalis.perfin.data.ulid.UlidCreator;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public record JdbcAttachmentRepository(Connection conn, Path contentDir) implements AttachmentRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcAttachmentRepository.class);

    @Override
    public Attachment insert(Path sourcePath) {
        String filename = sourcePath.getFileName().toString();
        String filetypeSuffix = FileUtil.getTypeSuffix(filename).toLowerCase();
        String contentType = FileUtil.MIMETYPES.getOrDefault(filetypeSuffix, "text/plain");
        Timestamp timestamp = DbUtil.timestampFromUtcNow();
        String identifier = UlidCreator.getUlid().toString();
        long id = DbUtil.insertOne(
                conn,
                "INSERT INTO attachment (uploaded_at, identifier, filename, content_type) VALUES (?, ?, ?, ?)",
                List.of(timestamp, identifier, filename, contentType)
        );
        Attachment attachment = new Attachment(id, DbUtil.utcLDTFromTimestamp(timestamp), identifier, filename, contentType);
        // Save the file to the content directory.
        Path storageFilePath = attachment.getPath(contentDir);
        try {
            Files.createDirectories(storageFilePath.getParent());
            Files.copy(sourcePath, storageFilePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return attachment;
    }

    @Override
    public Optional<Attachment> findById(long attachmentId) {
        return DbUtil.findOne(conn, "SELECT * FROM attachment WHERE id = ?", List.of(attachmentId), JdbcAttachmentRepository::parseAttachment);
    }

    @Override
    public Optional<Attachment> findByIdentifier(String identifier) {
        return DbUtil.findOne(conn, "SELECT * FROM attachment WHERE identifier = ?", List.of(identifier), JdbcAttachmentRepository::parseAttachment);
    }

    @Override
    public void deleteById(long attachmentId) {
        // First get it and try to delete the stored file.
        var optionalAttachment = findById(attachmentId);
        if (optionalAttachment.isPresent()) {
            try {
                Files.delete(optionalAttachment.get().getPath(contentDir));
            } catch (IOException e) {
                e.printStackTrace(System.err);
                // TODO: Add some sort of persistent error logging.
            }
            DbUtil.updateOne(conn, "DELETE FROM attachment WHERE id = ?", List.of(attachmentId));
        }
    }

    @Override
    public void deleteAllOrphans() {
        DbUtil.doTransaction(conn, () -> {
            List<Attachment> orphans = DbUtil.findAll(
                    conn,
                    """
                    SELECT * FROM attachment
                    WHERE
                        id NOT IN (SELECT attachment_id FROM transaction_attachment) AND
                        id NOT IN (SELECT attachment_id FROM balance_record_attachment)""",
                    JdbcAttachmentRepository::parseAttachment
            );
            for (Attachment orphan : orphans) {
                DbUtil.updateOne(
                        conn,
                        "DELETE FROM attachment WHERE id = ?",
                        List.of(orphan.id)
                );
                Path filePath = orphan.getPath(contentDir);
                try {
                    Files.deleteIfExists(filePath);
                    Path parentDir = filePath.getParent();
                    try (var filesRemaining = Files.list(parentDir)) {
                        if (filesRemaining.findAny().isEmpty()) {
                            Files.delete(parentDir);
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete attachment at " + filePath + ".", e);
                }
                log.debug("Deleted orphan attachment with id {} at {}.", orphan.id, filePath);
            }
        });
    }

    @Override
    public void close() throws Exception {
        conn.close();
    }

    public static Attachment parseAttachment(ResultSet rs) throws SQLException {
        return new Attachment(
                rs.getLong("id"),
                DbUtil.utcLDTFromTimestamp(rs.getTimestamp("uploaded_at")),
                rs.getString("identifier"),
                rs.getString("filename"),
                rs.getString("content_type")
        );
    }
}
