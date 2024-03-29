package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.Timestamped;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AccountRepository extends Repository, AutoCloseable {
    long insert(AccountType type, String accountNumber, String name, Currency currency, String description);
    Page<Account> findAll(PageRequest pagination);
    List<Account> findAllOrderedByRecentHistory();
    List<Account> findTopNOrderedByRecentHistory(int n);
    List<Account> findTopNRecentlyActive(int n, int daysSinceLastActive);
    List<Account> findAllByCurrency(Currency currency);
    Optional<Account> findById(long id);
    void update(long accountId, AccountType type, String accountNumber, String name, Currency currency, String description);
    void delete(Account account);
    void archive(long accountId);
    void unarchive(long accountId);

    BigDecimal deriveBalance(long accountId, Instant timestamp);
    default BigDecimal deriveCurrentBalance(long accountId) {
        return deriveBalance(accountId, Instant.now(Clock.systemUTC()));
    }
    Set<Currency> findAllUsedCurrencies();
    List<Timestamped> findEventsBefore(long accountId, LocalDateTime utcTimestamp, int maxResults);
}
