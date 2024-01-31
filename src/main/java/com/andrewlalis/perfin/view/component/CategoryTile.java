package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.EditCategoryController;
import com.andrewlalis.perfin.control.Popups;
import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.model.Profile;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CategoryTile extends VBox {
    public CategoryTile(
            TransactionCategoryRepository.CategoryTreeNode treeNode,
            Runnable categoriesRefresh
    ) {
        this.getStyleClass().addAll("tile", "hand-cursor");
        this.setStyle("-fx-border-width: 1px; -fx-border-color: grey;");
        this.setOnMouseClicked(event -> {
            event.consume();
            router.navigate(
                    "edit-category",
                    new EditCategoryController.CategoryRouteContext(treeNode.category())
            );
        });

        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().addAll("std-padding");
        Label nameLabel = new Label(treeNode.category().getName());
        nameLabel.getStyleClass().addAll("bold-text");
        Circle colorCircle = new Circle(10, treeNode.category().getColor());
        HBox contentBox = new HBox(colorCircle, nameLabel);
        contentBox.getStyleClass().addAll("std-spacing");
        borderPane.setLeft(contentBox);

        Button addChildButton = new Button("Add Subcategory");
        addChildButton.setOnAction(event -> router.navigate(
                "edit-category",
                new EditCategoryController.AddSubcategoryRouteContext(treeNode.category())
        ));
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> {
            boolean confirm = Popups.confirm(removeButton, "Are you sure you want to remove this category? It will permanently remove the category from all linked transactions, and all subcategories will also be removed. This cannot be undone.");
            if (confirm) {
                Profile.getCurrent().dataSource().useRepo(
                        TransactionCategoryRepository.class,
                        repo -> repo.deleteById(treeNode.category().id)
                );
                categoriesRefresh.run();
            }
        });
        HBox buttonsBox = new HBox(addChildButton, removeButton);
        buttonsBox.getStyleClass().addAll("std-spacing");
        borderPane.setRight(buttonsBox);

        this.getChildren().add(borderPane);
        for (var child : treeNode.children()) {
            this.getChildren().add(new CategoryTile(child, categoriesRefresh));
        }
    }
}
