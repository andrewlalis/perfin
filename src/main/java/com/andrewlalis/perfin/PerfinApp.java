package com.andrewlalis.perfin;

import com.andrewlalis.perfin.view.SplashScreenStage;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * The class from which the JavaFX-based application starts.
 */
public class PerfinApp extends Application {
    public static final Path APP_DIR = Path.of(System.getProperty("user.home", "."), ".perfin");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        SplashScreenStage splashStage = new SplashScreenStage("Loading", SceneUtil.load("/startup-splash-screen.fxml"));
        splashStage.show();
        initMainScreen(stage);
        splashStage.stateProperty().addListener((v, oldState, state) -> {
            if (state == SplashScreenStage.State.DONE) stage.show();
        });
    }

    private void initMainScreen(Stage stage) {
        stage.hide();
        stage.setScene(SceneUtil.load("/main.fxml"));
        stage.setTitle("Perfin");
    }
}