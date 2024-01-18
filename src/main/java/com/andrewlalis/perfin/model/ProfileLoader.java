package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.data.DataSourceFactory;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.util.FileUtil;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.andrewlalis.perfin.data.util.FileUtil.copyResourceFile;

public class ProfileLoader {
    private static final Logger log = LoggerFactory.getLogger(ProfileLoader.class);

    private final Window window;
    private final DataSourceFactory dataSourceFactory;

    public ProfileLoader(Window window, DataSourceFactory dataSourceFactory) {
        this.window = window;
        this.dataSourceFactory = dataSourceFactory;
    }

    public Profile load(String name) throws ProfileLoadException {
        if (Files.notExists(Profile.getDir(name))) {
            try {
                initProfileDir(name);
            } catch (IOException e) {
                FileUtil.deleteIfPossible(Profile.getDir(name));
                throw new ProfileLoadException("Failed to initialize new profile directory.", e);
            }
        }
        Properties settings = new Properties();
        try (var in = Files.newInputStream(Profile.getSettingsFile(name))) {
            settings.load(in);
        } catch (IOException e) {
            throw new ProfileLoadException("Failed to load profile settings.", e);
        }
        return new Profile(name, settings, dataSourceFactory.getDataSource(name));
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

    @Deprecated
    private static void initProfileDir(String name) throws IOException {
        Files.createDirectory(Profile.getDir(name));
        copyResourceFile("/text/profileDirReadme.txt", Profile.getDir(name).resolve("README.txt"));
        copyResourceFile("/text/defaultProfileSettings.properties", Profile.getSettingsFile(name));
        Files.createDirectory(Profile.getContentDir(name));
        copyResourceFile("/text/contentDirReadme.txt", Profile.getContentDir(name).resolve("README.txt"));
    }
}
