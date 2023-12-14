package com.andrewlalis.perfin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

public class SceneUtil {
    public static Scene load(String fxml, Object controller) {
        FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxml));
        if (controller != null) {
            if (loader.getController() != null) {
                throw new IllegalStateException("Cannot set loader for resource " + fxml + " because it has declared one already.");
            }
            loader.setController(controller);
        }
        try {
            return new Scene(loader.load());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Scene load(String fxml) {
        return load(fxml, null);
    }

    public static void addStylesheets(Scene scene, String... resources) {
        for (String resource : resources) {
            URL url = SceneUtil.class.getResource(resource);
            if (url == null) throw new RuntimeException("Could not load resource " + resource);
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
