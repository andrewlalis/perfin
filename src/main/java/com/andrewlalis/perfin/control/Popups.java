package com.andrewlalis.perfin.control;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

public class Popups {
    public static boolean confirm(String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text);
        alert.initModality(Modality.APPLICATION_MODAL);
        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
