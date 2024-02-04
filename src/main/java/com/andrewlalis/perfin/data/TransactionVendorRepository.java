package com.andrewlalis.perfin.data;

import com.andrewlalis.perfin.model.TransactionVendor;

import java.util.List;
import java.util.Optional;

public interface TransactionVendorRepository extends Repository, AutoCloseable {
    Optional<TransactionVendor> findById(long id);
    Optional<TransactionVendor> findByName(String name);
    List<TransactionVendor> findAll();
    long insert(String name, String description);
    long insert(String name);
    void update(long id, String name, String description);
    void deleteById(long id);
}
