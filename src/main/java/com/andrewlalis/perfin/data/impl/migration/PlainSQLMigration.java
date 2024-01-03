package com.andrewlalis.perfin.data.impl.migration;

import com.andrewlalis.perfin.data.impl.JdbcDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class PlainSQLMigration implements JdbcMigration {
    private final String resourceName;

    public PlainSQLMigration(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public void migrateJdbc(JdbcDataSource dataSource) throws Exception {
        try (
                var in = PlainSQLMigration.class.getResourceAsStream(resourceName);
                var conn = dataSource.getConnection();
                var stmt = conn.createStatement()
        ) {
            if (in == null) throw new IOException("Failed to load resource " + resourceName);
            String sqlString = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<String> sqlStatements = Arrays.stream(sqlString.split(";"))
                    .map(String::strip).filter(s -> !s.isBlank()).toList();
            System.out.println("Running SQL Migration with " + sqlStatements.size() + " statements:");
            for (String sqlStatement : sqlStatements) {
                System.out.println("  Executing SQL statement:\n" + sqlStatement + "\n-----\n");
                stmt.executeUpdate(sqlStatement);
            }
        }
    }
}
