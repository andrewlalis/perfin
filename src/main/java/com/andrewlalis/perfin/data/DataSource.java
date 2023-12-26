package com.andrewlalis.perfin.data;

public interface DataSource {
    AccountRepository getAccountRepository();
    default void useAccountRepository(ThrowableConsumer<AccountRepository> repoConsumer) {
        DbUtil.useClosable(this::getAccountRepository, repoConsumer);
    }

    BalanceRecordRepository getBalanceRecordRepository();
    default void useBalanceRecordRepository(ThrowableConsumer<BalanceRecordRepository> repoConsumer) {
        DbUtil.useClosable(this::getBalanceRecordRepository, repoConsumer);
    }
}
