package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.impl.JdbcDataSourceFactory;
import com.andrewlalis.perfin.data.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

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

    public static List<String> getAvailableProfiles() {
        try (var files = Files.list(PerfinApp.APP_DIR)) {
            return files.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted().toList();
        } catch (IOException e) {
            log.error("Failed to get a list of available profiles.", e);
            return Collections.emptyList();
        }
    }

    public static String getLastProfile() {
        Path lastProfileFile = PerfinApp.APP_DIR.resolve("last-profile.txt");
        if (Files.exists(lastProfileFile)) {
            try {
                String s = Files.readString(lastProfileFile).strip().toLowerCase();
                if (!s.isBlank()) return s;
            } catch (IOException e) {
                log.error("Failed to read " + lastProfileFile, e);
            }
        }
        return "default";
    }

    public static void saveLastProfile(String name) {
        Path lastProfileFile = PerfinApp.APP_DIR.resolve("last-profile.txt");
        try {
            Files.writeString(lastProfileFile, name);
        } catch (IOException e) {
            log.error("Failed to write " + lastProfileFile, e);
        }
    }

    public static void loadLast() throws ProfileLoadException {
        load(getLastProfile());
    }

    public static void load(String name) throws ProfileLoadException {
        if (Files.notExists(getDir(name))) {
            try {
                initProfileDir(name);
            } catch (IOException e) {
                FileUtil.deleteIfPossible(getDir(name));
                throw new ProfileLoadException("Failed to initialize new profile directory.", e);
            }
        }
        Properties settings = new Properties();
        try (var in = Files.newInputStream(getSettingsFile(name))) {
            settings.load(in);
        } catch (IOException e) {
            throw new ProfileLoadException("Failed to load profile settings.", e);
        }
        current = new Profile(name, settings, new JdbcDataSourceFactory().getDataSource(name));
        saveLastProfile(current.getName());
        for (var c : profileLoadListeners) {
            c.accept(current);
        }
    }

    private static void initProfileDir(String name) throws IOException {
        Files.createDirectory(getDir(name));
        copyResourceFile("/text/profileDirReadme.txt", getDir(name).resolve("README.txt"));
        copyResourceFile("/text/defaultProfileSettings.properties", getSettingsFile(name));
        Files.createDirectory(getContentDir(name));
        copyResourceFile("/text/contentDirReadme.txt", getContentDir(name).resolve("README.txt"));
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
        return name != null &&
                name.matches("\\w+") &&
                name.toLowerCase().equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
