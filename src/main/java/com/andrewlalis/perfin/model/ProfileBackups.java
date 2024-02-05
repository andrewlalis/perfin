package com.andrewlalis.perfin.model;

import com.andrewlalis.perfin.PerfinApp;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Helper class with static methods for managing backups of profiles.
 */
public class ProfileBackups {
    private static final Logger log = LoggerFactory.getLogger(ProfileBackups.class);

    public static Path getBackupDir(String profileName) {
        return PerfinApp.APP_DIR.resolve("backups").resolve(profileName);
    }

    public static Path makeBackup(String name) throws IOException {
        log.info("Making backup of profile \"{}\".", name);
        final Path profileDir = Profile.getDir(name);
        LocalDateTime now = LocalDateTime.now();
        Files.createDirectories(getBackupDir(name));
        Path backupFile = getBackupDir(name).resolve(String.format(
                "%04d-%02d-%02d_%02d-%02d-%02d.zip",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond()
        ));
        try (var out = new ZipOutputStream(Files.newOutputStream(backupFile))) {
            Files.walkFileTree(profileDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativeFile = profileDir.relativize(file);
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

    public static LocalDateTime getLastBackupTimestamp(String name) {
        try (var files = Files.list(getBackupDir(name))) {
            return files.map(ProfileBackups::getTimestampFromBackup)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        } catch (IOException e) {
            log.error("Failed to list files in profile " + name, e);
            return null;
        }
    }

    public static void cleanOldBackups(String name) {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        try (var files = Files.list(getBackupDir(name))) {
            var filesToDelete = files.filter(path -> {
                LocalDateTime timestamp = getTimestampFromBackup(path);
                return timestamp.isBefore(cutoff);
            }).toList();
            for (var file : filesToDelete) {
                Files.delete(file);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup backups.", e);
        }
    }

    private static LocalDateTime getTimestampFromBackup(Path backupFile) {
        String text = backupFile.getFileName().toString().substring(0, "0000-00-00_00-00-00".length());
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }
}
