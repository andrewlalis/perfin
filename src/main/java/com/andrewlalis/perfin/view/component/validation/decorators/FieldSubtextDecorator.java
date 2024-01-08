package com.andrewlalis.perfin.view.component.validation.decorators;

import com.andrewlalis.perfin.view.component.validation.ValidationDecorator;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validation decorator that can be applied to most controls, such that when
 * the field's value is invalid, messages are displayed in a label below the
 * field. This is accomplished by wrapping the control in a VBox only while
 * the value is invalid.
 */
public class FieldSubtextDecorator implements ValidationDecorator {
    private static final Logger log = LoggerFactory.getLogger(FieldSubtextDecorator.class);
    private static final String WRAP_KEY = FieldSubtextDecorator.class.getName();

    @Override
    public void decorate(Node node, ValidationResult result) {
        if (!result.isValid()) {
            if (!node.getStyleClass().contains("validation-field-invalid")) {
                node.getStyleClass().add("validation-field-invalid");
            }
            // Wrap the control in a VBox and put error messages under it.
            Label errorLabel;
            if (isNodeWrapped(node)) {
                VBox validationContainer = (VBox) node.getParent();
                errorLabel = (Label) validationContainer.getChildren().get(1);
            } else {
                errorLabel = wrapNode(node);
            }
            errorLabel.setText(result.asLines());
        } else {
            node.getStyleClass().remove("validation-field-invalid");
            // Unwrap the control, if it's wrapped.
            if (isNodeWrapped(node)) {
                unwrapNode(node);
            }
        }
    }

    private boolean isNodeWrapped(Node node) {
        return WRAP_KEY.equals(node.getParent().getUserData());
    }

    private Label wrapNode(Node node) {
        Pane trueParent = (Pane) node.getParent();
        int idx = trueParent.getChildren().indexOf(node);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().addAll("small-font", "negative-color-text-fill");
        errorLabel.setWrapText(true);
        VBox validationContainer = new VBox(node, errorLabel);
        validationContainer.setUserData(WRAP_KEY);
        trueParent.getChildren().add(idx, validationContainer);
        return errorLabel;
    }

    private void unwrapNode(Node node) {
        VBox validationContainer = (VBox) node.getParent();
        Pane trueParent = (Pane) node.getParent().getParent();
        int idx = trueParent.getChildren().indexOf(validationContainer);
        trueParent.getChildren().remove(idx);
        trueParent.getChildren().add(idx, node);
    }
}
