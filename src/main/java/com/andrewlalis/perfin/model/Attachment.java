package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.data.util.FileUtil;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An attachment is a file uploaded so that it may be related to some other
 * entity, like a receipt attached to a transaction, or a bank statement to an
 * account balance record.
 */
public class Attachment {
    private final long id;
    private final LocalDateTime timestamp;
    private final String identifier;
    private final String filename;
    private final String contentType;

    public Attachment(long id, LocalDateTime timestamp, String identifier, String filename, String contentType) {
        this.id = id;
        this.timestamp = timestamp;
        this.identifier = identifier;
        this.filename = filename;
        this.contentType = contentType;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Path getPath(Path contentDir) {
        return contentDir.resolve(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .resolve(identifier + FileUtil.getTypeSuffix(filename).toLowerCase());
    }
}
