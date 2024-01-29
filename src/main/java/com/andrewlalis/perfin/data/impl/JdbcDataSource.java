package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.*;
import com.andrewlalis.perfin.data.util.UncheckedSqlException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A basic data source implementation that gets a new SQL connection using a
 * pre-defined JDBC connection URL.
 */
public class JdbcDataSource implements DataSource {
    private final String jdbcUrl;
    private final Path contentDir;

    public JdbcDataSource(String jdbcUrl, Path contentDir) {
        this.jdbcUrl = jdbcUrl;
        this.contentDir = contentDir;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public Path getContentDir() {
        return contentDir;
    }

    @Override
    public AccountRepository getAccountRepository() {
        return new JdbcAccountRepository(getConnection(), contentDir);
    }

    @Override
    public BalanceRecordRepository getBalanceRecordRepository() {
        return new JdbcBalanceRecordRepository(getConnection(), contentDir);
    }

    @Override
    public TransactionRepository getTransactionRepository() {
        return new JdbcTransactionRepository(getConnection(), contentDir);
    }

    @Override
    public TransactionVendorRepository getTransactionVendorRepository() {
        return new JdbcTransactionVendorRepository(getConnection());
    }

    @Override
    public TransactionCategoryRepository getTransactionCategoryRepository() {
        return new JdbcTransactionCategoryRepository(getConnection());
    }

    @Override
    public AttachmentRepository getAttachmentRepository() {
        return new JdbcAttachmentRepository(getConnection(), contentDir);
    }

    @Override
    public AccountHistoryItemRepository getAccountHistoryItemRepository() {
        return new JdbcAccountHistoryItemRepository(getConnection());
    }
}
