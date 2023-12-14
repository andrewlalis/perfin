package com.andrewlalis.perfin.control;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class StartupSplashScreenController {
    @FXML
    public Label content;

    @FXML
    public void initialize() {
        content.setText("Loading Perfin...");
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(1000);

                Platform.runLater(() -> content.setText("Still loading..."));
                Thread.sleep(1000);

                Platform.runLater(() -> content.setText("Almost done..."));
                Thread.sleep(1000);

                Platform.runLater(() -> content.setText("Done!"));
                Thread.sleep(500);

                System.out.println("Closing splash screen...");
                Stage stage = (Stage) content.getScene().getWindow();
                Platform.runLater(stage::close);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
