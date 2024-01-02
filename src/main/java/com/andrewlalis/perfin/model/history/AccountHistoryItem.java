package com.andrewlalis.perfin.model.history;

import java.time.LocalDateTime;

/**
 * The base class representing account history items, a read-only record of an
 * account's data and changes over time. The type of history item determines
 * what exactly it means, and could be something like an account entry, balance
 * record, or modifications to the account's properties.
 */
public class AccountHistoryItem {
    private final long id;
    private final LocalDateTime timestamp;
    private final long accountId;
    private final AccountHistoryItemType type;

    public AccountHistoryItem(long id, LocalDateTime timestamp, long accountId, AccountHistoryItemType type) {
        this.id = id;
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getAccountId() {
        return accountId;
    }

    public AccountHistoryItemType getType() {
        return type;
    }
}
