package com.andrewlalis.perfin;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.javafx_scene_router.SceneRouter;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.StartupSplashScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

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
        var splashScreen = new StartupSplashScreen(List.of(
                PerfinApp::defineRoutes,
                PerfinApp::initAppDir,
                c -> initMainScreen(stage, c),
                PerfinApp::loadProfile
        ));
        splashScreen.showAndWait();
        if (splashScreen.isStartupSuccessful()) {
            stage.show();
        }
    }

    private void initMainScreen(Stage stage, Consumer<String> msgConsumer) throws Exception {
        msgConsumer.accept("Initializing main screen.");
        Platform.runLater(() -> {
            stage.hide();
            Scene mainViewScene = SceneUtil.load("/main-view.fxml");
            stage.setScene(mainViewScene);
            stage.setTitle("Perfin");
        });
    }

    private static void mapResourceRoute(String route, String resource) {
        router.map(route, PerfinApp.class.getResource(resource));
    }

    private static void defineRoutes(Consumer<String> msgConsumer) throws Exception {
        msgConsumer.accept("Initializing application views.");
        Platform.runLater(() -> {
            mapResourceRoute("accounts", "/accounts-view.fxml");
            mapResourceRoute("account", "/account-view.fxml");
            mapResourceRoute("edit-account", "/edit-account.fxml");
            mapResourceRoute("transactions", "/transactions-view.fxml");
            mapResourceRoute("create-transaction", "/create-transaction.fxml");
        });
    }

    private static void initAppDir(Consumer<String> msgConsumer) throws Exception {
        msgConsumer.accept("Validating application files.");
        if (Files.notExists(APP_DIR)) {
            msgConsumer.accept(APP_DIR + " doesn't exist yet. Creating it now.");
            Files.createDirectory(APP_DIR);
        } else if (Files.exists(APP_DIR) && Files.isRegularFile(APP_DIR)) {
            msgConsumer.accept(APP_DIR + " is a file, when it should be a directory. Deleting it and creating new directory.");
            Files.delete(APP_DIR);
            Files.createDirectory(APP_DIR);
        }
    }

    private static void loadProfile(Consumer<String> msgConsumer) throws Exception {
        msgConsumer.accept("Loading the most recent profile.");
        Profile.loadLast();
    }
}