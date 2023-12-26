package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.AccountEntry;

import java.util.List;

public interface AccountEntryRepository extends AutoCloseable {
    List<AccountEntry> findAllByAccountId(long accountId);
}
