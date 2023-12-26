package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends AutoCloseable {
    long insert(Account account);
    List<Account> findAll();
    Optional<Account> findById(long id);
    BigDecimal deriveCurrentBalance(long id);
    void update(Account account);
    void delete(Account account);
}
