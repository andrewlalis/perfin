package com.andrewlalis.perfin.data.util;

import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class FileUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public static Map<String, String> MIMETYPES = new HashMap<>();
    static {
        MIMETYPES.put(".pdf", "application/pdf");
        MIMETYPES.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIMETYPES.put(".odt", "application/vnd.oasis.opendocument.text");
        MIMETYPES.put(".html", "text/html");
        MIMETYPES.put(".txt", "text/plain");
        MIMETYPES.put(".md", "text/markdown");
        MIMETYPES.put(".xml", "application/xml");
        MIMETYPES.put(".json", "application/json");
        MIMETYPES.put(".png", "image/png");
        MIMETYPES.put(".jpg", "image/jpeg");
        MIMETYPES.put(".jpeg", "image/jpeg");
        MIMETYPES.put(".gif", "image/gif");
        MIMETYPES.put(".webp", "image/webp");
        MIMETYPES.put(".bmp", "image/bmp");
        MIMETYPES.put(".tiff", "image/tiff");
    }

    public static void deleteIfPossible(Path file) {
        try {
            if (Files.isDirectory(file)) {
                deleteDirRecursive(file);
            } else {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.error("Failed to delete " + file, e);
        }
    }

    public static void deleteDirRecursive(Path startDir) throws IOException {
        Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String getTypeSuffix(String filename) {
        int lastDotIdx = filename.lastIndexOf('.');
        if (lastDotIdx == -1) return "";
        return filename.substring(lastDotIdx);
    }

    public static String getTypeSuffix(Path filePath) {
        return getTypeSuffix(filePath.getFileName().toString());
    }

    public static FileChooser newAttachmentsFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachments");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Attachment Files",
                        "*.pdf", "*.docx", "*.odt", "*.html", "*.txt", "*.md", "*.xml", "*.json",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp", "*.tiff"
                )
        );
        return fileChooser;
    }
}
