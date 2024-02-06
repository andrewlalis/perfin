package com.andrewlalis.perfin.model.history;

import com.andrewlalis.perfin.model.IdEntity;
import com.andrewlalis.perfin.model.Timestamped;

import java.time.LocalDateTime;

/**
 * Represents a single polymorphic history item. The item's "type" attribute
 * tells where to find additional type-specific data.
 */
public abstract class HistoryItem extends IdEntity implements Timestamped {
    public enum Type {
        TEXT
    }

    private final long historyId;
    private final LocalDateTime timestamp;
    private final Type type;

    public HistoryItem(long id, long historyId, LocalDateTime timestamp, Type type) {
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

    public Type getType() {
        return type;
    }
}
