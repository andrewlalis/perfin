package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class TagsViewController implements RouteSelectionListener {
    @FXML public VBox tagsVBox;
    private final ObservableList<String> tags = FXCollections.observableArrayList();

    @FXML public void initialize() {
        BindingUtil.mapContent(tagsVBox.getChildren(), tags, this::buildTagTile);
    }

    @Override
    public void onRouteSelected(Object context) {
        refreshTags();
    }

    private void refreshTags() {
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionRepository.class,
                TransactionRepository::findAllTags
        ).thenAccept(strings -> Platform.runLater(() -> tags.setAll(strings)));
    }

    private Node buildTagTile(String name) {
        BorderPane tile = new BorderPane();
        tile.getStyleClass().addAll("tile");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().addAll("bold-text");
        Label usagesLabel = new Label();
        usagesLabel.getStyleClass().addAll("small-font", "secondary-color-text-fill");
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionRepository.class,
                repo -> repo.countTagUsages(name)
        ).thenAccept(count -> Platform.runLater(() -> usagesLabel.setText("Tagged transactions: " + count)));
        VBox contentBox = new VBox(nameLabel, usagesLabel);
        tile.setLeft(contentBox);
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> {
            boolean confirm = Popups.confirm(removeButton, "Are you sure you want to remove this tag? It will be removed from any transactions. This cannot be undone.");
            if (confirm) {
                Profile.getCurrent().dataSource().useRepo(
                        TransactionRepository.class,
                        repo -> repo.deleteTag(name)
                );
                refreshTags();
            }
        });
        tile.setRight(removeButton);
        return tile;
    }
}
