package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A basic data source implementation that gets a new SQL connection using a
 * pre-defined JDBC connection URL.
 */
public class JdbcDataSource implements DataSource {
    private final String jdbcUrl;

    public JdbcDataSource(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public AccountRepository getAccountRepository() {
        return new JdbcAccountRepository(getConnection());
    }

    @Override
    public BalanceRecordRepository getBalanceRecordRepository() {
        return new JdbcBalanceRecordRepository(getConnection());
    }

    @Override
    public TransactionRepository getTransactionRepository() {
        return new JdbcTransactionRepository(getConnection());
    }
}
