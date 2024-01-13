package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.beans.property.*;
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
import java.util.ArrayList;
import java.util.List;

/**
 * A pane within which a user can select one or more files.
 */
public class FileSelectionArea extends VBox {
    interface FileItem {
        String getName();
    }

    public record PathItem(Path path) implements FileItem {
        @Override
        public String getName() {
            return path.getFileName().toString();
        }
    }

    public record AttachmentItem(Attachment attachment) implements FileItem {
        @Override
        public String getName() {
            return attachment.getFilename();
        }
    }

    private final BooleanProperty allowMultiple = new SimpleBooleanProperty(false);

    private final ObservableList<FileItem> selectedFiles = FXCollections.observableArrayList();
    private final ObjectProperty<FileChooser> fileChooserProperty = new SimpleObjectProperty<>(getDefaultFileChooser());

    public FileSelectionArea() {
        getStyleClass().addAll("std-padding", "std-spacing");

        VBox filesVBox = new VBox();
        filesVBox.getStyleClass().addAll("std-padding", "std-spacing");
        BindingUtil.mapContent(filesVBox.getChildren(), selectedFiles, this::buildFileItem);
        ListProperty<FileItem> selectedFilesProperty = new SimpleListProperty<>(selectedFiles);

        Label noFilesLabel = new Label("No files selected.");
        noFilesLabel.managedProperty().bind(noFilesLabel.visibleProperty());
        noFilesLabel.visibleProperty().bind(selectedFilesProperty.emptyProperty());

        Button selectFilesButton = new Button("Select files");
        selectFilesButton.setOnAction(event -> {
            onSelectFileClicked(fileChooserProperty.get(), getScene().getWindow());
        });
        selectFilesButton.disableProperty().bind(
                allowMultiple.not().and(selectedFilesProperty.emptyProperty().not())
                        .or(fileChooserProperty.isNull())
        );

        getChildren().addAll(
                filesVBox,
                noFilesLabel,
                selectFilesButton
        );
    }

    public List<Attachment> getSelectedAttachments() {
        List<Attachment> attachments = new ArrayList<>();
        for (FileItem item : selectedFiles) {
            if (item instanceof AttachmentItem a) {
                attachments.add(a.attachment());
            }
        }
        return attachments;
    }

    public List<Path> getSelectedPaths() {
        List<Path> paths = new ArrayList<>();
        for (FileItem item : selectedFiles) {
            if (item instanceof PathItem p) {
                paths.add(p.path());
            }
        }
        return paths;
    }

    public void clear() {
        selectedFiles.clear();
    }

    public void addAttachments(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            FileItem item = new AttachmentItem(attachment);
            if (!selectedFiles.contains(item)) {
                selectedFiles.add(item);
            }
        }
    }

    public void setSelectedFiles(List<FileItem> files) {
        selectedFiles.setAll(files);
    }

    private Node buildFileItem(FileItem item) {
        Label filenameLabel = new Label(item.getName());
        filenameLabel.getStyleClass().addAll("mono-font");
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> selectedFiles.remove(item));
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
                    FileItem item = new PathItem(file.toPath());
                    if (!selectedFiles.contains(item)) {
                        selectedFiles.add(item);
                    }
                }
            }
        } else {
            File file = fileChooser.showOpenDialog(owner);
            FileItem item = file == null ? null : new PathItem(file.toPath());
            if (item != null && !selectedFiles.contains(item)) {
                selectedFiles.add(item);
            }
        }
    }

    private FileChooser getDefaultFileChooser() {
        return FileUtil.newAttachmentsFileChooser();
    }

    // Property methods.
    public final BooleanProperty allowMultipleProperty() {
        return allowMultiple;
    }

    public final boolean getAllowMultiple() {
        return allowMultiple.get();
    }

    public final void setAllowMultiple(boolean value) {
        allowMultiple.set(value);
    }
}
