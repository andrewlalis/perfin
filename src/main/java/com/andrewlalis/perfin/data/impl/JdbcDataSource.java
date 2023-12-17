package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.UncheckedSqlException;

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
}
