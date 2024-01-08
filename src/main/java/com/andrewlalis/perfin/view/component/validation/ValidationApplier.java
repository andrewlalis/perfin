package com.andrewlalis.perfin.view.component.validation;

import com.andrewlalis.perfin.view.component.validation.decorators.FieldSubtextDecorator;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.TextField;

/**
 * Fluent interface for applying a validator to one or more controls.
 * @param <T> The value type.
 */
public class ValidationApplier<T> {
    private final ValidationFunction<T> validator;
    private ValidationDecorator decorator = new FieldSubtextDecorator();
    private boolean validateInitially = false;

    public ValidationApplier(ValidationFunction<T> validator) {
        this.validator = validator;
    }

    public ValidationApplier<T> decoratedWith(ValidationDecorator decorator) {
        this.decorator = decorator;
        return this;
    }

    public ValidationApplier<T> validatedInitially() {
        this.validateInitially = true;
        return this;
    }

    public BooleanExpression attach(Node node, Property<T> valueProperty, Property<?>... triggerProperties) {
        BooleanExpression validProperty = BooleanExpression.booleanExpression(
                valueProperty.map(value -> validator.validate(value).isValid())
        );
        valueProperty.addListener((observable, oldValue, newValue) -> {
            ValidationResult result = validator.validate(newValue);
            decorator.decorate(node, result);
        });
        for (Property<?> influencingProperty : triggerProperties) {
            influencingProperty.addListener((observable, oldValue, newValue) -> {
                ValidationResult result = validator.validate(valueProperty.getValue());
                decorator.decorate(node, result);
            });
        }

        if (validateInitially) {
            // Call the decorator once to perform validation right away.
            decorator.decorate(node, validator.validate(valueProperty.getValue()));
        }
        return validProperty;
    }

    @SuppressWarnings("unchecked")
    public BooleanExpression attachToTextField(TextField field, Property<?>... triggerProperties) {
        return attach(field, (Property<T>) field.textProperty(), triggerProperties);
    }
}
