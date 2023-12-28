package com.andrewlalis.perfin.data;

import java.util.HashMap;
import java.util.Map;

public class FileUtil {
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
}
