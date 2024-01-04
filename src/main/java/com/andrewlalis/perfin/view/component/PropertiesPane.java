package com.andrewlalis.perfin.view.component;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import java.util.ArrayList;
import java.util.List;

/**
 * A specially-formatted {@link GridPane} that arranges its children into a
 * two-column grid representing key-value pairs.
 */
public class PropertiesPane extends GridPane {
    public PropertiesPane() {
        ColumnConstraints keyConstraints = new ColumnConstraints();
        keyConstraints.setHgrow(Priority.NEVER);
        keyConstraints.setHalignment(HPos.LEFT);
        keyConstraints.setMinWidth(10.0);
        ColumnConstraints valueConstraints = new ColumnConstraints();
        valueConstraints.setHgrow(Priority.ALWAYS);
        valueConstraints.setHalignment(HPos.LEFT);
        valueConstraints.setMinWidth(10.0);
        getColumnConstraints().setAll(keyConstraints, valueConstraints);
    }

    @Override
    protected void layoutChildren() {
        // Apply grid positioning to all children in the order in which they appear, like so:
        // key 1  value 1
        // key 2  value 2
        // ... and so on.
        int rowCount = getManagedChildren().size() / 2;
        List<RowConstraints> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            RowConstraints c = new RowConstraints();
            c.setValignment(VPos.TOP);
            c.setVgrow(Priority.NEVER);
            rows.add(c);
        }
        getRowConstraints().setAll(rows);
        for (int i = 0; i < getManagedChildren().size(); i++) {
            Node child = getManagedChildren().get(i);
            int column = i % 2;
            int row = i / 2;
            GridPane.setRowIndex(child, row);
            GridPane.setColumnIndex(child, column);
        }
        super.layoutChildren();
    }
}
