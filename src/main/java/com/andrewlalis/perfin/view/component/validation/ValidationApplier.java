package com.andrewlalis.perfin.view.component.validation;

import com.andrewlalis.perfin.view.component.validation.decorators.FieldSubtextDecorator;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;

/**
 * Fluent interface for applying a validator to one or more controls.
 * @param <T> The value type.
 */
public class ValidationApplier<T> {
    private final AsyncValidationFunction<T> validator;
    private ValidationDecorator decorator = new FieldSubtextDecorator();
    private boolean validateInitially = false;

    public ValidationApplier(ValidationFunction<T> validator) {
        this.validator = input -> CompletableFuture.completedFuture(validator.validate(input));
    }

    public ValidationApplier(AsyncValidationFunction<T> validator) {
        this.validator = validator;
    }

    public static <T> ValidationApplier<T> of(ValidationFunction<T> validator) {
        return new ValidationApplier<>(validator);
    }

    public static <T> ValidationApplier<T> ofAsync(AsyncValidationFunction<T> validator) {
        return new ValidationApplier<>(validator);
    }

    public ValidationApplier<T> decoratedWith(ValidationDecorator decorator) {
        this.decorator = decorator;
        return this;
    }

    public ValidationApplier<T> validatedInitially() {
        this.validateInitially = true;
        return this;
    }

    /**
     * Attaches the configured validator and decorator to a node, so that when
     * the node's specified valueProperty changes, the validator will be called
     * and if the new value is invalid, the decorator will update the UI to
     * show the message(s) to the user.
     * @param node The node to attach to.
     * @param valueProperty The property to listen for changes and validate on.
     * @param triggerProperties Additional properties that, when changed, can
     *                          trigger validation.
     * @return A boolean expression that tells whether the given valueProperty
     * is valid at any given time.
     */
    public BooleanExpression attach(Node node, Property<T> valueProperty, Property<?>... triggerProperties) {
        final SimpleBooleanProperty validProperty = new SimpleBooleanProperty();
        valueProperty.addListener((observable, oldValue, newValue) -> {
            validProperty.set(false); // Always set valid to false before we start validation.
            validator.validate(newValue)
                .thenAccept(result -> Platform.runLater(() -> {
                    validProperty.set(result.isValid());
                    decorator.decorate(node, result);
                }));
        });
        for (Property<?> influencingProperty : triggerProperties) {
            influencingProperty.addListener((observable, oldValue, newValue) -> {
                validProperty.set(false); // Always set valid to false before we start validation.
                validator.validate(valueProperty.getValue())
                    .thenAccept(result -> Platform.runLater(() -> {
                        validProperty.set(result.isValid());
                        decorator.decorate(node, result);
                    }));
            });
        }

        if (validateInitially) {
            // Call the decorator once to perform validation right away.
            validProperty.set(false); // Always set valid to false before we start validation.
            validator.validate(valueProperty.getValue())
                .thenAccept(result -> Platform.runLater(() -> {
                    validProperty.set(result.isValid());
                    decorator.decorate(node, result);
                }));
        }
        return validProperty;
    }

    @SuppressWarnings("unchecked")
    public BooleanExpression attachToTextField(TextField field, Property<?>... triggerProperties) {
        return attach(field, (Property<T>) field.textProperty(), triggerProperties);
    }
}
