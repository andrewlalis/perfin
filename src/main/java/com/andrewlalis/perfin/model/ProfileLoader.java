package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.control.Popups;
import com.andrewlalis.perfin.data.DataSourceFactory;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.impl.migration.Migrations;
import com.andrewlalis.perfin.data.util.FileUtil;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.andrewlalis.perfin.data.util.FileUtil.copyResourceFile;

/**
 * Component responsible for loading a profile from storage, as well as some
 * other basic tasks concerning the set of stored profiles.
 */
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
        try {
            DataSourceFactory.SchemaStatus status = dataSourceFactory.getSchemaStatus(name);
            if (status == DataSourceFactory.SchemaStatus.NEEDS_MIGRATION) {
                boolean confirm = Popups.confirm(window, "The profile \"" + name + "\" has an outdated data schema and needs to be migrated to the latest version. Is this okay?");
                if (!confirm) {
                    int existingSchemaVersion = dataSourceFactory.getSchemaVersion(name);
                    String compatibleVersion = Migrations.getLatestCompatibleVersion(existingSchemaVersion);
                    Popups.message(
                            window,
                            "The profile \"" + name + "\" is using schema version " + existingSchemaVersion + ", which is compatible with Perfin version " + compatibleVersion + ". Consider downgrading Perfin to access this profile safely."
                    );
                    throw new ProfileLoadException("User rejected the migration.");
                }
            } else if (status == DataSourceFactory.SchemaStatus.INCOMPATIBLE) {
                Popups.error(window, "The profile \"" + name + "\" has a data schema that's incompatible with this app. Update Perfin to access this profile safely.");
                throw new ProfileLoadException("Incompatible schema version.");
            }
        } catch (IOException e) {
            throw new ProfileLoadException("Failed to get profile's schema status.", e);
        }

        // Check for a recent backup and make one if not present.
        LocalDateTime lastBackup = getLastBackupTimestamp(name);
        if (lastBackup == null || lastBackup.isBefore(LocalDateTime.now().minusDays(5))) {
            try {
                makeBackup(name);
            } catch (IOException e) {
                log.error("Failed to create backup for profile " + name + ".", e);
            }
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

    public static LocalDateTime getLastBackupTimestamp(String name) {
        try (var files = Files.list(Profile.getDir(name))) {
            return files.filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .map(p -> p.getFileName().toString().substring("backup_".length(), "backup_0000-00-00_00-00-00".length()))
                    .map(s -> LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")))
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to list files in profile " + name, e);
            return null;
        }
    }

    public static Path makeBackup(String name) throws IOException {
        log.info("Making backup of profile \"{}\".", name);
        final Path profileDir = Profile.getDir(name);
        LocalDateTime now = LocalDateTime.now();
        Path backupFile = profileDir.resolve(String.format(
                "backup_%04d-%02d-%02d_%02d-%02d-%02d.zip",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond()
        ));
        try (var out = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            Files.walkFileTree(profileDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativeFile = profileDir.relativize(file);
                    if (relativeFile.toString().startsWith("backup_") || relativeFile.toString().equalsIgnoreCase("database.trace.db")) {
                        return FileVisitResult.CONTINUE;
                    }
                    out.putNextEntry(new ZipEntry(relativeFile.toString()));
                    byte[] bytes = Files.readAllBytes(file);
                    out.write(bytes, 0, bytes.length);
                    out.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return backupFile;
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
