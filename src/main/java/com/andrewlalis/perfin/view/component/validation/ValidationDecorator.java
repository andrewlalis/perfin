package com.andrewlalis.perfin.view.component.validation;

import javafx.scene.Node;

/**
 * A controller style component that, when validation of a field updates,
 * will represent the result in some way in the UI, usually by modifying the
 * given control and/or its parents in the scene graph.
 */
public interface ValidationDecorator {
    void decorate(Node node, ValidationResult result);
}
