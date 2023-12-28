package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.BalanceRecord;

public interface BalanceRecordRepository extends AutoCloseable {
    long insert(BalanceRecord record);
    BalanceRecord findLatestByAccountId(long accountId);
}
