package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.Popups;
import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionVendor;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class VendorTile extends BorderPane {
    public VendorTile(TransactionVendor vendor, Runnable vendorRefresh) {
        this.getStyleClass().addAll("tile", "std-spacing", "hand-cursor");
        this.setOnMouseClicked(event -> router.navigate("edit-vendor", vendor));

        Label nameLabel = new Label(vendor.getName());
        nameLabel.getStyleClass().addAll("bold-text");
        Label descriptionLabel = new Label(vendor.getDescription());
        descriptionLabel.setWrapText(true);
        VBox contentVBox = new VBox(nameLabel, descriptionLabel);
        contentVBox.getStyleClass().addAll("std-spacing");
        this.setCenter(contentVBox);
        BorderPane.setAlignment(contentVBox, Pos.TOP_LEFT);

        this.setRight(getRemoveButton(vendor, vendorRefresh));
    }

    private Button getRemoveButton(TransactionVendor transactionVendor, Runnable vendorRefresh) {
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> {
            boolean confirm = Popups.confirm(removeButton, "Are you sure you want to remove this vendor? Any transactions assigned to this vendor will have their vendor field cleared. This cannot be undone.");
            if (confirm) {
                Profile.getCurrent().dataSource().useRepo(
                        TransactionVendorRepository.class,
                        repo -> repo.deleteById(transactionVendor.id)
                );
                vendorRefresh.run();
            }
        });
        return removeButton;
    }
}
