package com.andrewlalis.perfin.control;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

/**
 * Helper class for standardized popups and confirmation dialogs for the app.
 */
public class Popups {
    public static boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text);
        alert.initModality(Modality.APPLICATION_MODAL);
        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static void message(String text) {
        Alert alert = new Alert(Alert.AlertType.NONE, text);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    public static void error(String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING, text);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }
}
