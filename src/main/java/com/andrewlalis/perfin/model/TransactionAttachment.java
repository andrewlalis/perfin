package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.data.DateUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A file that's been attached to a transaction as additional context for it,
 * like a receipt or invoice copy.
 */
@Deprecated
public class TransactionAttachment {
    private long id;
    private LocalDateTime uploadedAt;
    private long transactionId;

    private String filename;
    private String contentType;

    public TransactionAttachment(String filename, String contentType) {
        this.filename = filename;
        this.contentType = contentType;
    }

    public TransactionAttachment(long id, LocalDateTime uploadedAt, long transactionId, String filename, String contentType) {
        this.id = id;
        this.uploadedAt = uploadedAt;
        this.transactionId = transactionId;
        this.filename = filename;
        this.contentType = contentType;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Path getPath() {
        String uploadDateStr = uploadedAt.format(DateUtil.DEFAULT_DATE_FORMAT);
        return Profile.getContentDir(Profile.getCurrent().getName())
                .resolve("transaction-attachments")
                .resolve(uploadDateStr)
                .resolve("tx-" + transactionId)
                .resolve(filename);
    }
}
