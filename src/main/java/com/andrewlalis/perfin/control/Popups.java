package com.andrewlalis.perfin.control;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Helper class for standardized popups and confirmation dialogs for the app.
 */
public class Popups {
    public static boolean confirm(Window owner, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        var result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static boolean confirm(Node node, String text) {
        return confirm(getWindowFromNode(node), text);
    }

    public static void message(Window owner, String text) {
        Alert alert = new Alert(Alert.AlertType.NONE, text);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    public static void message(Node node, String text) {
        message(getWindowFromNode(node), text);
    }

    public static void error(Window owner, String text) {
        Alert alert = new Alert(Alert.AlertType.WARNING, text);
        alert.initOwner(owner);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait();
    }

    public static void error(Node node, String text) {
        error(getWindowFromNode(node), text);
    }

    public static void error(Window owner, Exception e) {
        error(owner, "An " + e.getClass().getSimpleName() + " occurred: " + e.getMessage());
    }

    public static void error(Node node, Exception e) {
        error(getWindowFromNode(node), e);
    }

    public static void errorLater(Node node, Exception e) {
        Platform.runLater(() -> error(node, e));
    }

    private static Window getWindowFromNode(Node n) {
        Window owner = null;
        Scene scene = n.getScene();
        if (scene != null) {
            owner = scene.getWindow();
        }
        return owner;
    }
}
