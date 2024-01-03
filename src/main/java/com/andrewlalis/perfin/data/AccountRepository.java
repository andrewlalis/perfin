package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AccountRepository extends AutoCloseable {
    long insert(AccountType type, String accountNumber, String name, Currency currency);
    Page<Account> findAll(PageRequest pagination);
    List<Account> findAllByCurrency(Currency currency);
    Optional<Account> findById(long id);
    void updateName(long id, String name);
    void update(Account account);
    void delete(Account account);
    void archive(Account account);

    BigDecimal deriveBalance(long id, Instant timestamp);
    default BigDecimal deriveCurrentBalance(long id) {
        return deriveBalance(id, Instant.now(Clock.systemUTC()));
    }
    Set<Currency> findAllUsedCurrencies();
}
