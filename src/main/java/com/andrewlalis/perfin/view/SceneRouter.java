package com.andrewlalis.perfin.view;

import com.andrewlalis.perfin.SceneUtil;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SceneRouter {
    private final Consumer<Parent> setter;
    private final Map<String, Parent> routeMap = new HashMap<>();
    public Object context = null;

    public SceneRouter(Pane pane) {
        this(p -> pane.getChildren().setAll(p));
    }

    public SceneRouter(Consumer<Parent> setter) {
        this.setter = setter;
    }

    public SceneRouter map(String route, Parent scene) {
        routeMap.put(route, scene);
        return this;
    }

    public SceneRouter map(String route, String fxml) {
        return map(route, SceneUtil.loadNode(fxml));
    }

    public void goTo(String route, Object context) {
        Parent node = routeMap.get(route);
        if (node == null) throw new IllegalArgumentException("Route " + route + " is not mapped to any node.");
        this.context = context;
        Platform.runLater(() -> setter.accept(node));
    }

    public void goTo(String route) {
        goTo(route, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext() {
        return (T) context;
    }
}
