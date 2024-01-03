package com.andrewlalis.perfin.data.impl.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Migrations {
    public static Map<Integer, Migration> getMigrations() {
        final Map<Integer, Migration> migrations = new HashMap<>();
        migrations.put(1, new PlainSQLMigration("/sql/migration/M1_AddBalanceRecordDeleted.sql"));
        return migrations;
    }

    public static List<Migration> getAll() {
        return getMigrations().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    public static Migration get(int currentVersion) {
        Migration selectedMigration = getMigrations().get(currentVersion);
        if (selectedMigration == null) {
            throw new IllegalArgumentException("No migration available from version " + currentVersion);
        }
        return selectedMigration;
    }
}
