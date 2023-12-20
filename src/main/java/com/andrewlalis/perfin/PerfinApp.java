package com.andrewlalis.perfin;

import com.andrewlalis.perfin.control.MainViewController;
import com.andrewlalis.perfin.view.SceneRouter;
import com.andrewlalis.perfin.view.SplashScreenStage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.function.Consumer;

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
            if (state == SplashScreenStage.State.ERROR) System.out.println("ERROR!");
        });
    }

    private void initMainScreen(Stage stage) {
        stage.hide();
        Scene mainViewScene = SceneUtil.load("/main-view.fxml", (Consumer<MainViewController>) c -> {
            c.router = new SceneRouter(c.mainContainer::setCenter)
                    .map("accounts", "/accounts-view.fxml")
                    .map("edit-account", "/edit-account.fxml");
        });
        stage.setScene(mainViewScene);
        stage.setTitle("Perfin");
    }
}