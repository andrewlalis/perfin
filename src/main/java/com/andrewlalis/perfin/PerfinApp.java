package com.andrewlalis.perfin;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.javafx_scene_router.SceneRouter;
import com.andrewlalis.perfin.view.SplashScreenStage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * The class from which the JavaFX-based application starts.
 */
public class PerfinApp extends Application {
    public static final Path APP_DIR = Path.of(System.getProperty("user.home", "."), ".perfin");

    /**
     * The router that's used for navigating between different "pages" in the application.
     */
    public static final SceneRouter router = new SceneRouter(new AnchorPaneRouterView(true));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        SplashScreenStage splashStage = new SplashScreenStage("Loading", SceneUtil.load("/startup-splash-screen.fxml"));
        splashStage.show();
        defineRoutes();
        initMainScreen(stage);
        splashStage.stateProperty().addListener((v, oldState, state) -> {
            if (state == SplashScreenStage.State.DONE) stage.show();
            if (state == SplashScreenStage.State.ERROR) System.out.println("ERROR!");
        });
    }

    private void initMainScreen(Stage stage) {
        stage.hide();
        Scene mainViewScene = SceneUtil.load("/main-view.fxml");
        stage.setScene(mainViewScene);
        stage.setTitle("Perfin");
    }

    private static void defineRoutes() {
        router.map("accounts", PerfinApp.class.getResource("/accounts-view.fxml"));
        router.map("account", PerfinApp.class.getResource("/account-view.fxml"));
        router.map("edit-account", PerfinApp.class.getResource("/edit-account.fxml"));
    }
}