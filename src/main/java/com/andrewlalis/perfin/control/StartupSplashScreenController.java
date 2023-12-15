package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.view.SplashScreenStage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.nio.file.Files;

import static com.andrewlalis.perfin.PerfinApp.APP_DIR;

/**
 * A controller for the application's splash screen that shows initially on
 * startup. While the splash screen is shown, we do any complicated loading
 * tasks so that the application starts properly, and give the user periodic
 * updates as we go.
 */
public class StartupSplashScreenController {
    @FXML
    public BorderPane sceneRoot;
    @FXML
    public TextArea content;

    @FXML
    public void initialize() {
        Thread.ofVirtual().start(() -> {
            try {
                printlnLater("Initializing application files...");
                if (!initAppDir()) {
                    Platform.runLater(() -> getSplashStage().setError());
                    return;
                }

                printlnLater("Perfin initialized. Starting the app now.");
                Thread.sleep(500);

                Platform.runLater(() -> getSplashStage().setDone());
            } catch (Exception e) {
                e.printStackTrace(System.err);
                printlnLater("An error occurred while starting: " + e.getMessage() + "\nThe application will now exit.");
                Platform.runLater(() -> getSplashStage().setError());
            }
        });
    }

    private void println(String text) {
        content.appendText(text + "\n");
    }

    private void printlnLater(String text) {
        Platform.runLater(() -> println(text));
    }

    private SplashScreenStage getSplashStage() {
        return (SplashScreenStage) sceneRoot.getScene().getWindow();
    }

    private boolean initAppDir() {
        if (Files.notExists(APP_DIR)) {
            printlnLater(APP_DIR + " doesn't exist yet. Creating it now.");
            try {
                Files.createDirectory(APP_DIR);
            } catch (IOException e) {
                printlnLater("Could not create directory " + APP_DIR + "; " + e.getMessage());
                return false;
            }
        } else if (Files.exists(APP_DIR) && Files.isRegularFile(APP_DIR)) {
            printlnLater(APP_DIR + " is a file, when it should be a directory. Deleting it and creating new directory.");
            try {
                Files.delete(APP_DIR);
                Files.createDirectory(APP_DIR);
            } catch (IOException e) {
                printlnLater("Could not delete file and create directory " + APP_DIR + "; " + e.getMessage());
                return false;
            }
        }
        return true;
    }
}
