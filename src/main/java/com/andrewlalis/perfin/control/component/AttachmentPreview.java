package com.andrewlalis.perfin.control.component;

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
    public AttachmentPreview(TransactionAttachment attachment) {
        Label nameLabel = new Label(attachment.getFilename());
        Label typeLabel = new Label(attachment.getContentType());
        typeLabel.setStyle("-fx-font-size: x-small;");
        setBottom(new VBox(nameLabel, typeLabel));

        Rectangle placeholder = new Rectangle(64.0, 64.0);
        placeholder.setFill(Color.WHITE);
        setCenter(placeholder);

        Set<String> imageTypes = Set.of("image/png", "image/jpeg", "image/gif", "image/bmp");
        if (imageTypes.contains(attachment.getContentType())) {
            try (var in = Files.newInputStream(attachment.getPath())) {
                Image img = new Image(in, 64.0, 64.0, true, true);
                setCenter(new ImageView(img));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
