package com.andrewlalis.perfin.view.component.module;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * A standardized header format for dashboard modules, which includes a left-aligned
 * title and right-aligned list of action items (usually buttons).
 */
public class ModuleHeader extends BorderPane {
    public ModuleHeader(String title, Node... actionItems) {
        this.getStyleClass().addAll("std-padding");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bold-text", "large-font");
        this.setLeft(titleLabel);

        HBox actionsHBox = new HBox();
        actionsHBox.getStyleClass().addAll("std-spacing", "small-font");
        actionsHBox.getChildren().addAll(actionItems);
        this.setRight(actionsHBox);
    }
}
