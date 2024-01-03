package com.andrewlalis.perfin.data.impl;

import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.impl.migration.Migration;
import com.andrewlalis.perfin.data.impl.migration.Migrations;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Component that's responsible for obtaining a JDBC data source for a profile.
 */
public class JdbcDataSourceFactory {
    private static final Logger log = LoggerFactory.getLogger(JdbcDataSourceFactory.class);

    /**
     * The version of schema that this app is compatible with. If a profile is
     * loaded with an old schema version, then we'll migrate to the latest. If
     * the profile has a newer schema version, we'll exit and prompt the user
     * to update their app.
     */
    public static final int SCHEMA_VERSION = 1;

    public DataSource getDataSource(String profileName) throws ProfileLoadException {
        final boolean dbExists = Files.exists(getDatabaseFile(profileName));
        if (!dbExists) {
            log.info("Creating new database for profile {}.", profileName);
            createNewDatabase(profileName);
        } else {
            int loadedSchemaVersion;
            try {
                loadedSchemaVersion = getSchemaVersion(profileName);
            } catch (IOException e) {
                log.error("Failed to load schema version.", e);
                throw new ProfileLoadException("Failed to determine database schema version.", e);
            }
            log.debug("Database loaded for profile {} has schema version {}.", profileName, loadedSchemaVersion);
            if (loadedSchemaVersion < SCHEMA_VERSION) {
                log.debug("Schema version {} is lower than the app's version {}. Performing migration.", loadedSchemaVersion, SCHEMA_VERSION);
                migrateToCurrentSchemaVersion(profileName, loadedSchemaVersion);
            } else if (loadedSchemaVersion > SCHEMA_VERSION) {
                log.debug("Schema version {} is higher than the app's version {}. Cannot continue.", loadedSchemaVersion, SCHEMA_VERSION);
                throw new ProfileLoadException("Profile " + profileName + " has a database with an unsupported schema version.");
            }
        }
        return new JdbcDataSource(getJdbcUrl(profileName), Profile.getContentDir(profileName));
    }

    private void createNewDatabase(String profileName) throws ProfileLoadException {
        log.info("Creating new database for profile {}.", profileName);
        JdbcDataSource dataSource = new JdbcDataSource(getJdbcUrl(profileName), Profile.getContentDir(profileName));
        try (
                InputStream in = JdbcDataSourceFactory.class.getResourceAsStream("/sql/schema.sql");
                Connection conn = dataSource.getConnection()
        ) {
            if (in == null) throw new IOException("Could not load database schema SQL file.");
            String schemaStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            executeSqlScript(schemaStr, conn);
            try {
                writeCurrentSchemaVersion(profileName);
            } catch (IOException e) {
                log.warn("Failed to write current schema version to file.", e);
            }
        } catch (IOException e) {
            log.error("IO Exception when trying to create database.", e);
            FileUtil.deleteIfPossible(getDatabaseFile(profileName));
            throw new ProfileLoadException("Failed to read SQL data to create database schema.", e);
        } catch (SQLException e) {
            log.error("SQL Exception when trying to create database.", e);
            FileUtil.deleteIfPossible(getDatabaseFile(profileName));
            throw new ProfileLoadException("Failed to create the database due to an SQL error.", e);
        }
        if (!testConnection(dataSource)) {
            FileUtil.deleteIfPossible(getDatabaseFile(profileName));
            throw new ProfileLoadException("Testing the database connection failed.");
        }
    }

    private boolean testConnection(JdbcDataSource dataSource) {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            return stmt.execute("SELECT 1;");
        } catch (SQLException e) {
            log.error("JDBC database connection failed.", e);
            return false;
        }
    }

    private void migrateToCurrentSchemaVersion(String profileName, int currentVersion) throws ProfileLoadException {
        // Before starting, copy the database file to a backup folder.
        Path backupDatabaseFile = getDatabaseFile(profileName).resolveSibling("migration-backup-database.mv.db");
        try {
            Files.copy(getDatabaseFile(profileName), backupDatabaseFile);
        } catch (IOException e) {
            throw new ProfileLoadException("Failed to prepare database backup prior to schema migration.", e);
        }
        int version = currentVersion;
        JdbcDataSource dataSource = new JdbcDataSource(getJdbcUrl(profileName), Profile.getContentDir(profileName));
        while (version < SCHEMA_VERSION) {
            log.info("Migrating profile {} from version {} to version {}.", profileName, version, version + 1);
            try {
                Migration m = Migrations.get(version);
                m.migrate(dataSource);
                version++;
            } catch (Exception e) {
                log.error("Migration from version " + version + " to " + (version+1) + " failed!", e);
                log.debug("Restoring database from pre-migration backup.");
                FileUtil.deleteIfPossible(getDatabaseFile(profileName));
                try {
                    Files.copy(backupDatabaseFile, getDatabaseFile(profileName));
                    FileUtil.deleteIfPossible(backupDatabaseFile);
                } catch (IOException e2) {
                    log.error("Failed to restore backup!", e2);
                    throw new ProfileLoadException("Failed to restore backup after a failed migration.", e2);
                }
                throw new ProfileLoadException("Migration failed and data restored to pre-migration state.", e);
            }
        }
        try {
            writeCurrentSchemaVersion(profileName);
        } catch (IOException e) {
            log.error("Failed to write current schema version after migration.");
            FileUtil.deleteIfPossible(getDatabaseFile(profileName));
            try {
                Files.copy(backupDatabaseFile, getDatabaseFile(profileName));
                FileUtil.deleteIfPossible(backupDatabaseFile);
            } catch (IOException e2) {
                throw new ProfileLoadException("Failed to restore backup after failing to set schema version.", e2);
            }
            throw new ProfileLoadException("Failed to update the schema version file after the migration.", e);
        }
        FileUtil.deleteIfPossible(backupDatabaseFile);
        log.info("Profile successfully migrated to latest version.");
    }

    private static void executeSqlScript(String script, Connection conn) throws SQLException {
        List<String> statements = Arrays.stream(script.split(";"))
                .map(String::strip).filter(s -> !s.isBlank()).toList();
        for (String statementText : statements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(statementText);
            }
        }
    }

    private static Path getDatabaseFile(String profileName) {
        return Profile.getDir(profileName).resolve("database.mv.db");
    }

    private static String getJdbcUrl(String profileName) {
        String dbPathAbs = getDatabaseFile(profileName).toAbsolutePath().toString();
        return "jdbc:h2:" + dbPathAbs.substring(0, dbPathAbs.length() - 6);
    }

    private static Path getSchemaVersionFile(String profileName) {
        return Profile.getDir(profileName).resolve(".jdbc-schema-version.txt");
    }

    private static int getSchemaVersion(String profileName) throws IOException {
        if (Files.exists(getSchemaVersionFile(profileName))) {
            try {
                return Integer.parseInt(Files.readString(getSchemaVersionFile(profileName)).strip());
            } catch (NumberFormatException e) {
                throw new IOException("Could not parse integer schema version.", e);
            }
        } else {
            writeCurrentSchemaVersion(profileName);
            return SCHEMA_VERSION;
        }
    }

    private static void writeCurrentSchemaVersion(String profileName) throws IOException {
        Files.writeString(getSchemaVersionFile(profileName), Integer.toString(SCHEMA_VERSION));
    }
}
