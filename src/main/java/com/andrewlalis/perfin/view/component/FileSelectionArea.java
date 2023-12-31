package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.view.BindingUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A pane within which a user can select one or more files.
 */
public class FileSelectionArea extends VBox {
    public final BooleanProperty allowMultiple = new SimpleBooleanProperty(false);
    private final ObservableList<Path> selectedFiles = FXCollections.observableArrayList();

    public FileSelectionArea(Supplier<FileChooser> fileChooserSupplier, Supplier<Window> windowSupplier) {
        getStyleClass().addAll("std-padding", "std-spacing");

        VBox filesVBox = new VBox();
        filesVBox.getStyleClass().addAll("std-padding", "std-spacing");
        BindingUtil.mapContent(filesVBox.getChildren(), selectedFiles, this::buildFileItem);
        ListProperty<Path> selectedFilesProperty = new SimpleListProperty<>(selectedFiles);

        Label noFilesLabel = new Label("No files selected.");
        noFilesLabel.managedProperty().bind(noFilesLabel.visibleProperty());
        noFilesLabel.visibleProperty().bind(selectedFilesProperty.emptyProperty());

        Button selectFilesButton = new Button("Select files");
        selectFilesButton.setOnAction(event -> onSelectFileClicked(fileChooserSupplier.get(), windowSupplier.get()));
        selectFilesButton.disableProperty().bind(
                allowMultiple.not().and(selectedFilesProperty.emptyProperty().not())
        );

        getChildren().addAll(
                filesVBox,
                noFilesLabel,
                selectFilesButton
        );
    }

    public List<Path> getSelectedFiles() {
        return Collections.unmodifiableList(selectedFiles);
    }

    public void clear() {
        selectedFiles.clear();
    }

    private Node buildFileItem(Path path) {
        Label filenameLabel = new Label(path.getFileName().toString());
        filenameLabel.getStyleClass().addAll("mono-font");
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> selectedFiles.remove(path));
        AnchorPane pane = new AnchorPane(filenameLabel, removeButton);
        AnchorPane.setLeftAnchor(filenameLabel, 0.0);
        AnchorPane.setTopAnchor(filenameLabel, 0.0);
        AnchorPane.setBottomAnchor(filenameLabel, 0.0);

        AnchorPane.setRightAnchor(removeButton, 0.0);
        return pane;
    }

    private void onSelectFileClicked(FileChooser fileChooser, Window owner) {
        if (allowMultiple.get()) {
            var files = fileChooser.showOpenMultipleDialog(owner);
            if (files != null) {
                for (File file : files) {
                    Path path = file.toPath();
                    if (!selectedFiles.contains(path)) {
                        selectedFiles.add(path);
                    }
                }
            }
        } else {
            File file = fileChooser.showOpenDialog(owner);
            if (file != null && !selectedFiles.contains(file.toPath())) {
                selectedFiles.add(file.toPath());
            }
        }
    }
}
