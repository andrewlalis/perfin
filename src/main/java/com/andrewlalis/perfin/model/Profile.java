package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.impl.JdbcDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * A profile is essentially a complete set of data that the application can
 * operate on, sort of like a save file or user account. The profile contains
 * a set of accounts, transaction records, attached documents, historical data,
 * and more. A profile can be imported or exported easily from the application,
 * and can be encrypted for additional security. Each profile also has its own
 * settings.
 * <p>
 *     Practically, each profile is a directory containing a database file,
 *     settings, files, and other information.
 * </p>
 * <p>
 *     Because only one profile may be loaded in the app at once, the Profile
 *     class maintains a static <em>current</em> profile that can be loaded and
 *     unloaded.
 * </p>
 */
public class Profile {
    private static Profile current;
    private static final List<Consumer<Profile>> profileLoadListeners = new ArrayList<>();

    private final String name;
    private final Properties settings;
    private final DataSource dataSource;

    private Profile(String name, Properties settings, DataSource dataSource) {
        this.name = name;
        this.settings = settings;
        this.dataSource = dataSource;
    }

    public String getName() {
        return name;
    }

    public Properties getSettings() {
        return settings;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public static Path getDir(String name) {
        return PerfinApp.APP_DIR.resolve(name);
    }

    public static Path getContentDir(String name) {
        return getDir(name).resolve("content");
    }

    public static Path getSettingsFile(String name) {
        return getDir(name).resolve("settings.properties");
    }

    public static Path getDatabaseFile(String name) {
        return getDir(name).resolve("database.mv.db");
    }

    public static Profile getCurrent() {
        return current;
    }

    public static void whenLoaded(Consumer<Profile> consumer) {
        if (current != null) {
            consumer.accept(current);
        } else {
            profileLoadListeners.add(consumer);
        }
    }

    public static String getLastProfile() {
        Path lastProfileFile = PerfinApp.APP_DIR.resolve("last-profile.txt");
        if (Files.exists(lastProfileFile)) {
            try {
                String s = Files.readString(lastProfileFile).strip().toLowerCase();
                if (!s.isBlank()) return s;
            } catch (IOException e) {
                System.err.println("Failed to read " + lastProfileFile);
                e.printStackTrace(System.err);
            }
        }
        return "default";
    }

    public static void saveLastProfile(String name) {
        Path lastProfileFile = PerfinApp.APP_DIR.resolve("last-profile.txt");
        try {
            Files.writeString(lastProfileFile, name);
        } catch (IOException e) {
            System.err.println("Failed to write " + lastProfileFile);
            e.printStackTrace(System.err);
        }
    }

    public static void loadLast() throws Exception {
        load(getLastProfile());
    }

    public static void load(String name) throws IOException {
        if (Files.notExists(getDir(name))) {
            initProfileDir(name);
        }
        Properties settings = new Properties();
        try (var in = Files.newInputStream(getSettingsFile(name))) {
            settings.load(in);
        }
        current = new Profile(name, settings, initJdbcDataSource(name));
        saveLastProfile(current.getName());
        for (var c : profileLoadListeners) {
            c.accept(current);
        }
        profileLoadListeners.clear();
    }

    private static void initProfileDir(String name) throws IOException {
        Files.createDirectory(getDir(name));
        copyResourceFile("/text/profileDirReadme.txt", getDir(name).resolve("README.txt"));
        copyResourceFile("/text/defaultProfileSettings.properties", getSettingsFile(name));
        Files.createDirectory(getContentDir(name));
        copyResourceFile("/text/contentDirReadme.txt", getContentDir(name).resolve("README.txt"));
    }

    private static DataSource initJdbcDataSource(String name) throws IOException {
        String databaseFilename = getDatabaseFile(name).toAbsolutePath().toString();
        String jdbcUrl = "jdbc:h2:" + databaseFilename.substring(0, databaseFilename.length() - 6);
        boolean exists = Files.exists(getDatabaseFile(name));
        JdbcDataSource dataSource = new JdbcDataSource(jdbcUrl);
        if (!exists) {// Initialize the datasource using schema.sql.
            try (var in = Profile.class.getResourceAsStream("/sql/schema.sql"); var conn = dataSource.getConnection()) {
                if (in == null) throw new IOException("Could not load /sql/schema.sql");
                String schemaStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                List<String> statements = Arrays.stream(schemaStr.split(";"))
                        .map(String::strip).filter(s -> !s.isBlank()).toList();
                for (var statementStr : statements) {
                    try (var stmt = conn.createStatement()) {
                        stmt.executeUpdate(statementStr);
                        System.out.println("Executed update:\n" + statementStr + "\n-----");
                    }
                }
            } catch (SQLException e) {
                Files.deleteIfExists(getDatabaseFile(name));
                throw new IOException("Failed to initialize database.", e);
            }
        }
        // Test the datasource before returning it.
        try (var conn = dataSource.getConnection(); var s = conn.createStatement()) {
            boolean success = s.execute("SELECT 1;");
            if (!success) throw new IOException("Failed to execute DB test statement.");
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return dataSource;
    }

    private static void copyResourceFile(String resource, Path dest) throws IOException {
        try (
                var in = Profile.class.getResourceAsStream(resource);
                var out = Files.newOutputStream(dest)
        ) {
            if (in == null) throw new IOException("Could not load resource " + resource);
            in.transferTo(out);
        }
    }

    public static boolean validateName(String name) {
        return name.matches("\\w+");
    }
}
