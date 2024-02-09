package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public class CategoryLabel extends HBox {
    public CategoryLabel(TransactionCategory category) {
        this(category, 8);
    }

    public CategoryLabel(TransactionCategory category, double indicatorSize) {
        Circle colorIndicator = new Circle(8, category.getColor());
        Label label = new Label(category.getName());
        this.getChildren().addAll(colorIndicator, label);
        this.getStyleClass().add("std-spacing");
    }
}
