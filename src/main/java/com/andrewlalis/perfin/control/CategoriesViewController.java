package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.CategoryTile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CategoriesViewController implements RouteSelectionListener {
    @FXML public VBox categoriesVBox;
    private final ObservableList<TransactionCategoryRepository.CategoryTreeNode> categoryTreeNodes = FXCollections.observableArrayList();

    @FXML public void initialize() {
        BindingUtil.mapContent(categoriesVBox.getChildren(), categoryTreeNodes, node -> new CategoryTile(node, this::refreshCategories));
    }

    @Override
    public void onRouteSelected(Object context) {
        refreshCategories();
    }

    @FXML public void addCategory() {
        router.navigate("edit-category");
    }

    private void refreshCategories() {
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionCategoryRepository.class,
                TransactionCategoryRepository::findTree
        ).thenAccept(nodes -> Platform.runLater(() -> categoryTreeNodes.setAll(nodes)));
    }
}
