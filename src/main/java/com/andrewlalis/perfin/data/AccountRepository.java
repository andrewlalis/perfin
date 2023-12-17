package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    long insert(Account account);
    List<Account> findAll();
    Optional<Account> findById(long id);
}
