package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.MoneyValue;
import javafx.application.Platform;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
    TransactionVendorRepository getTransactionVendorRepository();
    TransactionCategoryRepository getTransactionCategoryRepository();
    AttachmentRepository getAttachmentRepository();
    HistoryRepository getHistoryRepository();

    // Repository helper methods:

    @SuppressWarnings("unchecked")
    default <R extends Repository, T> T mapRepo(Class<R> repoType, Function<R, T> action) {
        Supplier<R> repoSupplier = getRepo(repoType);
        if (repoSupplier == null) throw new IllegalArgumentException("Repository type " + repoType + " is not supported.");
        boolean repoCloseable = Arrays.asList(repoType.getInterfaces()).contains(AutoCloseable.class);
        if (repoCloseable) {
            try (AutoCloseable c = (AutoCloseable) repoSupplier.get()) {
                R repo = (R) c;
                return action.apply(repo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            R repo = repoSupplier.get();
            return action.apply(repo);
        }
    }

    default <R extends Repository, T> CompletableFuture<T> mapRepoAsync(Class<R> repoType, Function<R, T> action) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            cf.complete(mapRepo(repoType, action));
        });
        return cf;
    }

    default <R extends Repository> void useRepo(Class<R> repoType, Consumer<R> action) {
        mapRepo(repoType, (Function<R, Void>) repo -> {
            action.accept(repo);
            return null;
        });
    }

    default <R extends Repository> CompletableFuture<Void> useRepoAsync(Class<R> repoType, Consumer<R> action) {
        return mapRepoAsync(repoType, repo -> {
            action.accept(repo);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private <R extends Repository> Supplier<R> getRepo(Class<R> type) {
        final Map<Class<? extends Repository>, Supplier<? extends Repository>> repoSuppliers = Map.of(
                AccountRepository.class, this::getAccountRepository,
                BalanceRecordRepository.class, this::getBalanceRecordRepository,
                TransactionRepository.class, this::getTransactionRepository,
                TransactionVendorRepository.class, this::getTransactionVendorRepository,
                TransactionCategoryRepository.class, this::getTransactionCategoryRepository,
                AttachmentRepository.class, this::getAttachmentRepository,
                HistoryRepository.class, this::getHistoryRepository
        );
        return (Supplier<R>) repoSuppliers.get(type);
    }

    // Utility methods:

    default CompletableFuture<String> getAccountBalanceText(Account account) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        mapRepoAsync(AccountRepository.class, repo -> {
            BigDecimal balance = repo.deriveCurrentBalance(account.id);
            MoneyValue money = new MoneyValue(balance, account.getCurrency());
            return CurrencyUtil.formatMoney(money);
        }).thenAccept(s -> Platform.runLater(() -> cf.complete(s)));
        return cf;
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
