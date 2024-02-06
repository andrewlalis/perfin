package com.andrewlalis.perfin.view.component.module;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * A container intended to display content on the app's dashboard, where modules
 * are arranged in a flexible grid based on some constraints.
 */
public class DashboardModule extends VBox {
    public final IntegerProperty columnsProperty = new SimpleIntegerProperty(1);
    public final DoubleProperty minColumnWidthProperty = new SimpleDoubleProperty(500);
    public final DoubleProperty rowHeightProperty = new SimpleDoubleProperty(500);

    public DashboardModule(Pane parent) {
        ObservableValue<Double> dynamicWidth = parent.widthProperty().map(parentWidth -> {
            double parentWidthD = parentWidth.doubleValue();
            if (parentWidthD < minColumnWidthProperty.doubleValue() * columnsProperty.doubleValue()) return parentWidthD;
            return Math.floor(parentWidthD / columnsProperty.doubleValue());
        });
        this.minWidthProperty().bind(dynamicWidth);
        this.prefWidthProperty().bind(dynamicWidth);
        this.maxWidthProperty().bind(dynamicWidth);

        this.minHeightProperty().bind(rowHeightProperty);
        this.prefHeightProperty().bind(rowHeightProperty);
        this.maxHeightProperty().bind(rowHeightProperty);

        this.getStyleClass().add("std-padding");
    }

    public void refreshContents() {}
}
