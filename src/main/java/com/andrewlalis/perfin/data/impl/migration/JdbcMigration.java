package com.andrewlalis.perfin.data.impl.migration;

import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.impl.JdbcDataSource;

public interface JdbcMigration extends Migration {
    default void migrate(DataSource dataSource) throws Exception {
        if (dataSource instanceof JdbcDataSource ds) {
            migrateJdbc(ds);
        } else {
            throw new IllegalArgumentException("This migration only accepts JDBC data sources.");
        }
    }
    void migrateJdbc(JdbcDataSource dataSource) throws Exception;
}
