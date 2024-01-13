package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * A pane which shows a list of attachments in a horizontally scrolling
 * container.
 */
public class AttachmentsViewPane extends VBox {
    private final StringProperty titleProperty = new SimpleStringProperty("Attachments");
    private final ObservableList<Attachment> attachments = FXCollections.observableArrayList();
    private final ListProperty<Attachment> attachmentListProperty = new SimpleListProperty<>(attachments);

    public AttachmentsViewPane() {
        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("bold-text");
        titleLabel.textProperty().bind(titleProperty);

        HBox attachmentsHBox = new HBox();
        attachmentsHBox.setMinHeight(AttachmentPreview.HEIGHT);
        attachmentsHBox.setPrefHeight(AttachmentPreview.HEIGHT);
        attachmentsHBox.setMaxHeight(AttachmentPreview.HEIGHT);
        attachmentsHBox.getStyleClass().add("std-spacing");
        BindingUtil.mapContent(attachmentsHBox.getChildren(), attachments, AttachmentPreview::new);

        ScrollPane scrollPane = new ScrollPane(attachmentsHBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.minViewportHeightProperty().bind(attachmentsHBox.heightProperty());
        scrollPane.prefViewportHeightProperty().bind(attachmentsHBox.heightProperty());

        getChildren().addAll(titleLabel, scrollPane);
    }

    public void setTitle(String title) {
        titleProperty.set(title);
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments.clear();
        this.attachments.setAll(attachments);
    }

    public ListProperty<Attachment> listProperty() {
        return attachmentListProperty;
    }

    public void hideIfEmpty() {
        managedProperty().bind(visibleProperty());
        visibleProperty().bind(attachmentListProperty.emptyProperty().not());
    }
}
