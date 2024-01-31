package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionCategory;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.util.concurrent.CompletableFuture;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditCategoryController implements RouteSelectionListener {
    public record CategoryRouteContext(TransactionCategory category) implements RouteContext {}
    public record AddSubcategoryRouteContext(TransactionCategory parent) implements RouteContext {}
    private sealed interface RouteContext permits AddSubcategoryRouteContext, CategoryRouteContext {}

    private TransactionCategory category;
    private TransactionCategory parent;

    @FXML public TextField nameField;
    @FXML public ColorPicker colorPicker;

    @FXML public Button saveButton;

    @FXML public void initialize() {
        var nameValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s != null && !s.isBlank(), "Name is required.")
                .addPredicate(s -> s.strip().length() <= TransactionCategory.NAME_MAX_LENGTH, "Name is too long.")
                .addAsyncPredicate(
                        s -> {
                            if (Profile.getCurrent() == null) return CompletableFuture.completedFuture(false);
                            return Profile.getCurrent().dataSource().mapRepoAsync(
                                    TransactionCategoryRepository.class,
                                    repo -> {
                                        var categoryByName = repo.findByName(s).orElse(null);
                                        if (this.category != null) {
                                            return this.category.equals(categoryByName) || categoryByName == null;
                                        }
                                        return categoryByName == null;
                                    }
                            );
                        },
                        "Category with this name already exists."
                )
        ).validatedInitially().attachToTextField(nameField);

        saveButton.disableProperty().bind(nameValid.not());
    }

    @Override
    public void onRouteSelected(Object context) {
        this.category = null;
        this.parent = null;
        if (context instanceof RouteContext ctx) {
            switch (ctx) {
                case CategoryRouteContext(var cat):
                    this.category = cat;
                    nameField.setText(cat.getName());
                    colorPicker.setValue(cat.getColor());
                    break;
                case AddSubcategoryRouteContext(var par):
                    this.parent = par;
                    nameField.setText(null);
                    colorPicker.setValue(parent.getColor());
                    break;
            }
        } else {
            nameField.setText(null);
            colorPicker.setValue(Color.WHITE);
        }
    }

    @FXML public void save() {
        final String name = nameField.getText().strip();
        final Color color = colorPicker.getValue();
        if (this.category == null && this.parent == null) {
            // New top-level category.
            Profile.getCurrent().dataSource().useRepo(
                    TransactionCategoryRepository.class,
                    repo -> repo.insert(name, color)
            );
        } else if (this.category == null) {
            // New subcategory.
            Profile.getCurrent().dataSource().useRepo(
                    TransactionCategoryRepository.class,
                    repo -> repo.insert(parent.id, name, color)
            );
        } else if (this.parent == null) {
            // Save edits to an existing category.
            Profile.getCurrent().dataSource().useRepo(
                    TransactionCategoryRepository.class,
                    repo -> repo.update(category.id, name, color)
            );
        }
        router.replace("categories");
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }
}
