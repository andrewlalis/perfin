package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.BalanceRecord;

public interface BalanceRecordRepository extends AutoCloseable {
    BalanceRecord findLatestByAccountId(long accountId);
}
