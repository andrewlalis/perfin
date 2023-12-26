package com.andrewlalis.perfin.data;

public interface DataSource {
    AccountRepository getAccountRepository();
    default void useAccountRepository(ThrowableConsumer<AccountRepository> repoConsumer) {
        DbUtil.useClosable(this::getAccountRepository, repoConsumer);
    }
}
