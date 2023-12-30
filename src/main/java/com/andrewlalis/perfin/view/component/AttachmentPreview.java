package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.TransactionAttachment;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

/**
 * A small component that shows the basic information about an attachment,
 * like its name, type, and a preview image if possible.
 */
public class AttachmentPreview extends BorderPane {
    public static final double IMAGE_SIZE = 64.0;
    public static final double LABEL_SIZE = 18.0;
    public static final double HEIGHT = IMAGE_SIZE + LABEL_SIZE;

    public AttachmentPreview(TransactionAttachment attachment) {
        Label nameLabel = new Label(attachment.getFilename());
        nameLabel.setStyle("-fx-font-size: small;");
        VBox nameContainer = new VBox(nameLabel);
        nameContainer.setPrefHeight(LABEL_SIZE);
        nameContainer.setMaxHeight(LABEL_SIZE);
        nameContainer.setMinHeight(LABEL_SIZE);
        setBottom(nameContainer);

        Rectangle placeholder = new Rectangle(IMAGE_SIZE, IMAGE_SIZE);
        placeholder.setFill(Color.WHITE);
        setCenter(placeholder);

        Set<String> imageTypes = Set.of("image/png", "image/jpeg", "image/gif", "image/bmp");
        if (imageTypes.contains(attachment.getContentType())) {
            try (var in = Files.newInputStream(attachment.getPath())) {
                Image img = new Image(in, IMAGE_SIZE, IMAGE_SIZE, true, true);
                setCenter(new ImageView(img));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
