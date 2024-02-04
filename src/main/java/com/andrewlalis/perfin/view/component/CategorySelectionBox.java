package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategorySelectionBox extends ComboBox<TransactionCategory> {
    private final Map<TransactionCategory, Integer> categoryIndentationLevels = new HashMap<>();

    public CategorySelectionBox() {
        setCellFactory(view -> new CategoryListCell(categoryIndentationLevels));
        setButtonCell(new CategoryListCell(null));
    }

    public void loadCategories(List<TransactionCategoryRepository.CategoryTreeNode> treeNodes) {
        categoryIndentationLevels.clear();
        getItems().clear();
        populateCategories(treeNodes, 0);
        getItems().add(null);
    }

    private void populateCategories(
            List<TransactionCategoryRepository.CategoryTreeNode> treeNodes,
            int depth
    ) {
        for (var node : treeNodes) {
            getItems().add(node.category());
            categoryIndentationLevels.put(node.category(), depth);
            populateCategories(node.children(), depth + 1);
        }
    }

    public void select(TransactionCategory category) {
        setButtonCell(new CategoryListCell(null));
        getSelectionModel().select(category);
    }

    private static class CategoryListCell extends ListCell<TransactionCategory> {
        private final Label nameLabel = new Label();
        private final Circle colorIndicator = new Circle(8);
        private final Map<TransactionCategory, Integer> categoryIndentationLevels;

        public CategoryListCell(Map<TransactionCategory, Integer> categoryIndentationLevels) {
            this.categoryIndentationLevels = categoryIndentationLevels;
            nameLabel.getStyleClass().add("normal-color-text-fill");
            colorIndicator.managedProperty().bind(colorIndicator.visibleProperty());
            HBox container = new HBox(colorIndicator, nameLabel);
            container.getStyleClass().add("std-spacing");
            setGraphic(container);
        }

        @Override
        protected void updateItem(TransactionCategory item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                nameLabel.setText("None");
                colorIndicator.setVisible(false);
                return;
            }

            nameLabel.setText(item.getName());
            if (categoryIndentationLevels != null) {
                HBox.setMargin(
                        colorIndicator,
                        new Insets(0, 0, 0, 10 * categoryIndentationLevels.getOrDefault(item, 0))
                );
            }
            colorIndicator.setVisible(true);
            colorIndicator.setFill(item.getColor());
        }
    }
}
