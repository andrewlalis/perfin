package com.andrewlalis.perfin.view;

import com.andrewlalis.perfin.data.util.Pair;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Consumer;

public class SceneUtil {
    public static <N, C> Pair<N, C> loadNodeAndController(String fxml) {
        FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxml));
        try {
            N node = loader.load();
            C controller = loader.getController();
            return Pair.of(node, controller);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> Parent loadNode(String fxml, Consumer<T> controllerConfig) {
        FXMLLoader loader = new FXMLLoader(SceneUtil.class.getResource(fxml));
        try {
            Parent p = loader.load();
            if (controllerConfig != null) {
                T controller = loader.getController();
                if (controller == null) throw new NullPointerException("Could not get controller from " + fxml);
                controllerConfig.accept(controller);
            }
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Parent loadNode(String fxml) {
        return loadNode(fxml, null);
    }

    public static <T> Scene load(String fxml, Consumer<T> controllerConfig) {
        return new Scene(loadNode(fxml, controllerConfig));
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
