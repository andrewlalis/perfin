package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionVendor;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditVendorController implements RouteSelectionListener {
    private TransactionVendor vendor;

    @FXML public TextField nameField;
    @FXML public TextArea descriptionField;
    @FXML public Button saveButton;

    @FXML public void initialize() {
        var nameValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s != null && !s.isBlank(), "Name is required.")
                .addPredicate(s -> s.strip().length() <= TransactionVendor.NAME_MAX_LENGTH, "Name is too long.")
                // A predicate that prevents duplicate names.
                .addAsyncPredicate(
                        s -> {
                            if (Profile.getCurrent() == null) return CompletableFuture.completedFuture(false);
                            return Profile.getCurrent().dataSource().mapRepoAsync(
                                    TransactionVendorRepository.class,
                                    repo -> {
                                        var vendorByName = repo.findByName(s).orElse(null);
                                        if (this.vendor != null) {
                                            return this.vendor.equals(vendorByName) || vendorByName == null;
                                        }
                                        return vendorByName == null;
                                    }
                            );
                        },
                        "Vendor with this name already exists."
                )
            ).validatedInitially().attachToTextField(nameField);
        var descriptionValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addPredicate(
                        s -> s == null || s.strip().length() <= TransactionVendor.DESCRIPTION_MAX_LENGTH,
                        "Description is too long."
                )
            ).validatedInitially().attach(descriptionField, descriptionField.textProperty());

        var formValid = nameValid.and(descriptionValid);
        saveButton.disableProperty().bind(formValid.not());
    }

    @Override
    public void onRouteSelected(Object context) {
        if (context instanceof TransactionVendor tv) {
            this.vendor = tv;
            nameField.setText(vendor.getName());
            descriptionField.setText(vendor.getDescription());
        } else {
            nameField.setText(null);
            descriptionField.setText(null);
        }
    }

    @FXML public void save() {
        String name = nameField.getText().strip();
        String description = descriptionField.getText() == null ? null : descriptionField.getText().strip();
        DataSource ds = Profile.getCurrent().dataSource();
        if (vendor != null) {
            ds.useRepo(TransactionVendorRepository.class, repo -> repo.update(vendor.id, name, description));
        } else {
            ds.useRepo(TransactionVendorRepository.class, repo -> {
                if (description == null || description.isEmpty()) {
                    repo.insert(name);
                } else {
                    repo.insert(name, description);
                }
            });
        }
        router.replace("vendors");
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }
}
