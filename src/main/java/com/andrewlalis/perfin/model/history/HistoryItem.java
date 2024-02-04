package com.andrewlalis.perfin.model.history;

import com.andrewlalis.perfin.model.IdEntity;

import java.time.LocalDateTime;

/**
 * Represents a single polymorphic history item. The item's "type" attribute
 * tells where to find additional type-specific data.
 */
public abstract class HistoryItem extends IdEntity {
    public static final String TYPE_TEXT = "TEXT";

    private final long historyId;
    private final LocalDateTime timestamp;
    private final String type;

    public HistoryItem(long id, long historyId, LocalDateTime timestamp, String type) {
        super(id);
        this.historyId = historyId;
        this.timestamp = timestamp;
        this.type = type;
    }

    public long getHistoryId() {
        return historyId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }
}
