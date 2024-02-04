package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionVendor;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.VendorTile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class VendorsViewController implements RouteSelectionListener {
    @FXML public VBox vendorsVBox;
    private final ObservableList<TransactionVendor> vendors = FXCollections.observableArrayList();

    @FXML public void initialize() {
        BindingUtil.mapContent(vendorsVBox.getChildren(), vendors, vendor -> new VendorTile(vendor, this::refreshVendors));
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
