package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.impl.JdbcDataSource;
import com.andrewlalis.perfin.data.impl.JdbcDataSourceFactory;
import com.andrewlalis.perfin.data.util.DbUtil;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.CategoryTile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.UncheckedIOException;

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

    @FXML public void addDefaultCategories() {
        boolean confirm = Popups.confirm(categoriesVBox, "Are you sure you want to add all of Perfin's default categories to your profile? This might interfere with existing categories of the same name.");
        if (!confirm) return;
        JdbcDataSource ds = (JdbcDataSource) Profile.getCurrent().dataSource();
        try (var conn = ds.getConnection()) {
            DbUtil.doTransaction(conn, () -> {
                try {
                    new JdbcDataSourceFactory().insertDefaultCategories(conn);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            refreshCategories();
        } catch (Exception e) {
            Popups.error(categoriesVBox, e);
        }
    }
}
