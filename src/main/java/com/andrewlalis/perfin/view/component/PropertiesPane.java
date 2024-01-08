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
 * two-column grid representing key-value pairs. It will use a default set of
 * {@link ColumnConstraints} unless they are already defined by the user.
 */
public class PropertiesPane extends GridPane {
    private final ColumnConstraints defaultKeyColumnConstraints;
    private final ColumnConstraints defaultValueColumnConstraints;
    private boolean columnConstraintsSet = false;

    public PropertiesPane(int keyColumnMinWidth) {
        defaultKeyColumnConstraints = new ColumnConstraints();
        defaultKeyColumnConstraints.setHgrow(Priority.NEVER);
        defaultKeyColumnConstraints.setHalignment(HPos.LEFT);
        if (keyColumnMinWidth != -1) {
            defaultKeyColumnConstraints.setMinWidth(keyColumnMinWidth);
        }
        defaultValueColumnConstraints = new ColumnConstraints();
        defaultValueColumnConstraints.setHgrow(Priority.ALWAYS);
        defaultValueColumnConstraints.setHalignment(HPos.LEFT);
    }

    public PropertiesPane() {
        this(-1);
    }

    @Override
    protected void layoutChildren() {
        // Apply grid positioning to all children in the order in which they appear, like so:
        // key 1  value 1
        // key 2  value 2
        // ... and so on.

        // Set column restraints if they weren't set already.
        if (!columnConstraintsSet) {
            if (getColumnConstraints().isEmpty()) {
                // No preexisting constraints, so we set our defaults.
                getColumnConstraints().setAll(defaultKeyColumnConstraints, defaultValueColumnConstraints);
            }
            columnConstraintsSet = true;
        }

        // Set row constraints for each row.
        int rowCount = getManagedChildren().size() / 2;
        List<RowConstraints> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            RowConstraints c = new RowConstraints();
            c.setValignment(VPos.TOP);
            c.setVgrow(Priority.NEVER);
            rows.add(c);
        }
        getRowConstraints().setAll(rows);

        // Set child row and column indices.
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
