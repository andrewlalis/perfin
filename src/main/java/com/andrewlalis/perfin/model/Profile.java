package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
 *
 * @param name The name of the profile.
 * @param settings The profile's settings.
 * @param dataSource The profile's data source.
 */
public record Profile(String name, Properties settings, DataSource dataSource) {
    private static final Logger log = LoggerFactory.getLogger(Profile.class);

    private static Profile current;
    private static final Set<WeakReference<Consumer<Profile>>> currentProfileListeners = new HashSet<>();

    public void setSettingAndSave(String settingName, String value) {
        String previous = settings.getProperty(settingName);
        if (Objects.equals(previous, value)) return; // Value is already set.
        settings.setProperty(settingName, value);
        try (var out = Files.newOutputStream(getSettingsFile(name))) {
            settings.store(out, null);
        } catch (IOException e) {
            log.error("Failed to save settings.", e);
        }
    }

    public Optional<String> getSetting(String settingName) {
        return Optional.ofNullable(settings.getProperty(settingName));
    }

    @Override
    public String toString() {
        return name;
    }

    public static Path getProfilesDir() {
        return PerfinApp.APP_DIR.resolve("profiles");
    }

    public static Path getDir(String name) {
        return getProfilesDir().resolve(name);
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

    public static void setCurrent(Profile profile) {
        current = profile;
        for (var ref : currentProfileListeners) {
            Consumer<Profile> consumer = ref.get();
            if (consumer != null) {
                consumer.accept(profile);
            }
        }
        currentProfileListeners.removeIf(ref -> ref.get() == null);
        log.debug("Current profile set to {}.", current.name());
    }

    public static void whenLoaded(Consumer<Profile> consumer) {
        if (current != null) {
            consumer.accept(current);
        }
        currentProfileListeners.add(new WeakReference<>(consumer));
    }

    public static boolean validateName(String name) {
        return name != null &&
                name.matches("\\w+") &&
                name.toLowerCase().equals(name);
    }
}
