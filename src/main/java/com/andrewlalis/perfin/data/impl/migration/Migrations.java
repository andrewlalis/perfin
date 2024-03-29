package com.andrewlalis.perfin.data.impl.migration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for defining and using all known migrations.
 */
public class Migrations {
    /**
     * Gets a list of migrations, as a map with the key being the version to
     * migrate from. For example, a migration that takes us from version 42 to
     * 43 would exist in the map with key 42.
     * @return The map of all migrations.
     */
    public static Map<Integer, Migration> getMigrations() {
        final Map<Integer, Migration> migrations = new HashMap<>();
        migrations.put(1, new PlainSQLMigration("/sql/migration/M001_AddTransactionProperties.sql"));
        migrations.put(2, new PlainSQLMigration("/sql/migration/M002_RefactorHistories.sql"));
        migrations.put(3, new PlainSQLMigration("/sql/migration/M003_AddLineItemCategoryAndAccountDescription.sql"));
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

    public static Map<Integer, String> getSchemaVersionCompatibility() {
        final Map<Integer, String> compatibilities = new HashMap<>();
        compatibilities.put(1, "1.4.0");
        return compatibilities;
    }

    public static String getLatestCompatibleVersion(int schemaVersion) {
        return getSchemaVersionCompatibility().get(schemaVersion);
    }
}
