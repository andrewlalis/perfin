package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.ImageCache;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * A small component that shows the basic information about an attachment,
 * like its name, type, and a preview image if possible.
 */
public class AttachmentPreview extends BorderPane {
    public static final double IMAGE_SIZE = 64.0;
    public static final double LABEL_SIZE = 18.0;
    public static final double HEIGHT = IMAGE_SIZE + LABEL_SIZE + 6;

    public AttachmentPreview(Attachment attachment) {
        BorderPane contentContainer = new BorderPane();
        contentContainer.setPadding(new Insets(3));

        Label nameLabel = new Label(attachment.getFilename());
        nameLabel.getStyleClass().add("small-font");
        nameLabel.setPrefHeight(LABEL_SIZE);
        nameLabel.setMaxHeight(LABEL_SIZE);
        nameLabel.setMinHeight(LABEL_SIZE);
        nameLabel.setMaxWidth(2 * IMAGE_SIZE);
        nameLabel.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(nameLabel, Pos.CENTER);
        contentContainer.setBottom(nameLabel);

        boolean showDocIcon = true;
        Set<String> imageTypes = Set.of("image/png", "image/jpeg", "image/gif", "image/bmp");
        if (imageTypes.contains(attachment.getContentType())) {
            try (var in = Files.newInputStream(attachment.getPath(Profile.getContentDir(Profile.getCurrent().getName())))) {
                Image img = new Image(in, IMAGE_SIZE, IMAGE_SIZE, true, true);
                contentContainer.setCenter(new ImageView(img));
                showDocIcon = false;
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (showDocIcon) {
            contentContainer.setCenter(new ImageView(ImageCache.instance.get("/images/doc-icon.png", 64, 64, true, true)));
        }

        BorderPane hoverIndicatorPane = new BorderPane();
        hoverIndicatorPane.prefWidthProperty().bind(contentContainer.widthProperty());
        hoverIndicatorPane.prefHeightProperty().bind(contentContainer.heightProperty());
        hoverIndicatorPane.setBackground(new Background(new BackgroundFill(Color.rgb(186, 210, 255, 0.5), null, null)));
        hoverIndicatorPane.visibleProperty().bind(this.hoverProperty());

        StackPane stackPane = new StackPane(contentContainer, hoverIndicatorPane);

        this.setCenter(stackPane);
        this.setOnMouseClicked(event -> {
            if (this.isHover()) {
                Path filePath = attachment.getPath(Profile.getContentDir(Profile.getCurrent().getName()));
                PerfinApp.instance.getHostServices().showDocument(filePath.toAbsolutePath().toUri().toString());
            }
        });
    }
}
