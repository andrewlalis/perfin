package com.andrewlalis.perfin.model.history;

import java.time.LocalDateTime;

public class HistoryTextItem extends HistoryItem {
    private final String description;

    public HistoryTextItem(long id, long historyId, LocalDateTime timestamp, String description) {
        super(id, historyId, timestamp, HistoryItem.TYPE_TEXT);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
