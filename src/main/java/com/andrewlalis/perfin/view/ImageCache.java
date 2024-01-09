package com.andrewlalis.perfin.view;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    public static final ImageCache instance = new ImageCache();

    private final Map<String, Image> images = new ConcurrentHashMap<>();

    public Image get(String resource, double width, double height, boolean preserveRatio, boolean smooth) {
        final String cacheKey = getCacheKey(resource, width, height, preserveRatio, smooth);
        Image stored = images.get(cacheKey);
        if (stored != null) return stored;
        try (var in = ImageCache.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Could not load resource " + resource);
            Image img = new Image(in, width, height, preserveRatio, smooth);
            images.put(cacheKey, img);
            return img;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getCacheKey(String resource, double width, double height, boolean preserveRatio, boolean smooth) {
        return resource + "_" +
                "W" + width + "_" +
                "H" + height + "_" +
                "PR-" + preserveRatio + "_" +
                "S-" + smooth;
    }

    public static Image getLogo256() {
        return instance.get("/images/perfin-logo_256.png", 256, 256, true, true);
    }
}
