package com.andrewlalis.perfin;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PerfinApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initMainScreen(stage);
        Stage splashStage = showStartupSplashScreen();
        // Once the splash stage is hidden, show the main stage.
        splashStage.showingProperty().not().addListener((v, old, hidden) -> {
            if (hidden) {
                showMainScreen(stage);
            }
        });
    }

    private void initMainScreen(Stage stage) {
        stage.hide();
        stage.setScene(SceneUtil.load("/main.fxml"));
        stage.setTitle("Perfin");
    }

    private void showMainScreen(Stage stage) {
        System.out.println("Showing the main application.");
//        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Shows a startup "splash" screen for a short time.
     * @return The stage in which the splash screen is shown.
     */
    private Stage showStartupSplashScreen() {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(SceneUtil.load("/startup-splash-screen.fxml"));
        stage.setTitle("Loading");
        stage.setResizable(false);
        stage.show();
        return stage;
    }
}