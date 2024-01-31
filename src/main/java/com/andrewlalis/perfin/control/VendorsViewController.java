package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionVendor;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class VendorsViewController implements RouteSelectionListener {
    @FXML public VBox vendorsVBox;
    private final ObservableList<TransactionVendor> vendors = FXCollections.observableArrayList();

    @FXML public void initialize() {
        BindingUtil.mapContent(vendorsVBox.getChildren(), vendors, this::buildVendorTile);
    }

    private Node buildVendorTile(TransactionVendor transactionVendor) {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().addAll("tile", "std-spacing");
        pane.setOnMouseClicked(event -> router.navigate("edit-vendor", transactionVendor));

        Label nameLabel = new Label(transactionVendor.getName());
        nameLabel.getStyleClass().addAll("bold-text");
        Label descriptionLabel = new Label(transactionVendor.getDescription());
        descriptionLabel.setWrapText(true);
        VBox contentVBox = new VBox(nameLabel, descriptionLabel);
        contentVBox.getStyleClass().addAll("std-spacing");
        pane.setCenter(contentVBox);
        BorderPane.setAlignment(contentVBox, Pos.TOP_LEFT);

        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> {
            boolean confirm = Popups.confirm(removeButton, "Are you sure you want to remove this vendor? Any transactions with assigned to this vendor will have their vendor field cleared. This cannot be undone.");
            if (confirm) {
                Profile.getCurrent().dataSource().useRepo(TransactionVendorRepository.class, repo -> {
                    repo.deleteById(transactionVendor.id);
                });
                refreshVendors();
            }
        });
        pane.setRight(removeButton);

        return pane;
    }

    @Override
    public void onRouteSelected(Object context) {
        refreshVendors();
    }

    @FXML public void addVendor() {
        router.navigate("edit-vendor");
    }

    private void refreshVendors() {
        Profile.getCurrent().dataSource().useRepoAsync(TransactionVendorRepository.class, repo -> {
            final List<TransactionVendor> vendors = repo.findAll();
            Platform.runLater(() -> {
                this.vendors.clear();
                this.vendors.addAll(vendors);
            });
        });
    }
}
