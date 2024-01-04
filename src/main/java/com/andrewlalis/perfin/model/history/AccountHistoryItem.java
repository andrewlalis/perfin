package com.andrewlalis.perfin.model.history;

import com.andrewlalis.perfin.model.IdEntity;

import java.time.LocalDateTime;

/**
 * The base class representing account history items, a read-only record of an
 * account's data and changes over time. The type of history item determines
 * what exactly it means, and could be something like an account entry, balance
 * record, or modifications to the account's properties.
 */
public class AccountHistoryItem extends IdEntity {
    private final LocalDateTime timestamp;
    private final long accountId;
    private final AccountHistoryItemType type;

    public AccountHistoryItem(long id, LocalDateTime timestamp, long accountId, AccountHistoryItemType type) {
        super(id);
        this.timestamp = timestamp;
        this.accountId = accountId;
        this.type = type;
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
