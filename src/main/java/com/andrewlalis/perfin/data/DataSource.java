package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.data.util.ThrowableConsumer;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.MoneyValue;
import javafx.application.Platform;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interface for methods to obtain any data from a {@link com.andrewlalis.perfin.model.Profile}
 * instance. Usually, you'll obtain a repository to interact with entities of a
 * certain type.
 */
public interface DataSource {
    /**
     * Gets the directory in which file content is stored.
     * @return The content directory.
     */
    Path getContentDir();

    AccountRepository getAccountRepository();
    BalanceRecordRepository getBalanceRecordRepository();
    TransactionRepository getTransactionRepository();
    AttachmentRepository getAttachmentRepository();
    AccountHistoryItemRepository getAccountHistoryItemRepository();

    default void useAccountRepository(ThrowableConsumer<AccountRepository> repoConsumer) {
        DbUtil.useClosable(this::getAccountRepository, repoConsumer);
    }

    default void useBalanceRecordRepository(ThrowableConsumer<BalanceRecordRepository> repoConsumer) {
        DbUtil.useClosable(this::getBalanceRecordRepository, repoConsumer);
    }

    default void useTransactionRepository(ThrowableConsumer<TransactionRepository> repoConsumer) {
        DbUtil.useClosable(this::getTransactionRepository, repoConsumer);
    }

    default void useAttachmentRepository(ThrowableConsumer<AttachmentRepository> repoConsumer) {
        DbUtil.useClosable(this::getAttachmentRepository, repoConsumer);
    }

    // Utility methods:

    default void getAccountBalanceText(Account account, Consumer<String> balanceConsumer) {
        Thread.ofVirtual().start(() -> useAccountRepository(repo -> {
            BigDecimal balance = repo.deriveCurrentBalance(account.id);
            MoneyValue money = new MoneyValue(balance, account.getCurrency());
            Platform.runLater(() -> balanceConsumer.accept(CurrencyUtil.formatMoney(money)));
        }));
    }

    default Map<Currency, BigDecimal> getCombinedAccountBalances() {
        try (var accountRepo = getAccountRepository()) {
            List<Account> accounts = accountRepo.findAll(PageRequest.unpaged()).items();
            Map<Currency, BigDecimal> totals = new HashMap<>();
            for (var account : accounts) {
                BigDecimal currencyTotal = totals.computeIfAbsent(account.getCurrency(), c -> BigDecimal.ZERO);
                BigDecimal accountBalance = accountRepo.deriveCurrentBalance(account.id);
                if (account.getType() == AccountType.CREDIT_CARD) accountBalance = accountBalance.negate();
                totals.put(account.getCurrency(), currencyTotal.add(accountBalance));
            }
            return totals;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
