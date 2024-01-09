package com.andrewlalis.perfin.view.component;

import com.andrewlalis.javafx_scene_router.RouterView;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

public class ScrollPaneRouterView implements RouterView {
    private final ScrollPane scrollPane = new ScrollPane();

    public ScrollPaneRouterView() {
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
    }

    @Override
    public void showRouteNode(Parent node) {
        scrollPane.setContent(node);
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }
}
