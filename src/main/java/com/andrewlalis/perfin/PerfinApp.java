package com.andrewlalis.perfin;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.javafx_scene_router.SceneRouter;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.ImageCache;
import com.andrewlalis.perfin.view.SceneUtil;
import com.andrewlalis.perfin.view.StartupSplashScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * The class from which the JavaFX-based application starts.
 */
public class PerfinApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(PerfinApp.class);
    public static final Path APP_DIR = Path.of(System.getProperty("user.home", "."), ".perfin");
    public static PerfinApp instance;

    /**
     * The router that's used for navigating between different "pages" in the application.
     */
    public static final SceneRouter router = new SceneRouter(new AnchorPaneRouterView(true));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        loadFonts();
        var splashScreen = new StartupSplashScreen(List.of(
                PerfinApp::defineRoutes,
                PerfinApp::initAppDir,
                c -> initMainScreen(stage, c),
                PerfinApp::loadLastUsedProfile
        ));
        splashScreen.showAndWait();
        if (splashScreen.isStartupSuccessful()) {
            stage.show();
            stage.setMaximized(true);
        }
    }

    private void initMainScreen(Stage stage, Consumer<String> msgConsumer) {
        msgConsumer.accept("Initializing main screen.");
        Platform.runLater(() -> {
            stage.hide();
            Scene mainViewScene = SceneUtil.load("/main-view.fxml");
            stage.setScene(mainViewScene);
            stage.setTitle("Perfin");
            stage.getIcons().add(ImageCache.getLogo64());
        });
    }

    private static void mapResourceRoute(String route, String resource) {
        router.map(route, PerfinApp.class.getResource(resource));
    }

    private static void defineRoutes(Consumer<String> msgConsumer) {
        msgConsumer.accept("Initializing application views.");
        Platform.runLater(() -> {
            mapResourceRoute("accounts", "/accounts-view.fxml");
            mapResourceRoute("account", "/account-view.fxml");
            mapResourceRoute("edit-account", "/edit-account.fxml");
            mapResourceRoute("transactions", "/transactions-view.fxml");
            mapResourceRoute("create-transaction", "/create-transaction.fxml");
            mapResourceRoute("create-balance-record", "/create-balance-record.fxml");
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

    private static void loadLastUsedProfile(Consumer<String> msgConsumer) throws Exception {
        msgConsumer.accept("Loading the most recent profile.");
        try {
            Profile.loadLast();
        } catch (ProfileLoadException e) {
            msgConsumer.accept("Failed to load the profile: " + e.getMessage());
            throw e;
        }
    }

    private static void loadFonts() {
        List<String> fontResources = List.of(
                "/font/JetBrainsMono-2.304/fonts/ttf/JetBrainsMono-Medium.ttf",
                "/font/Roboto/Roboto-Regular.ttf",
                "/font/Roboto/Roboto-Bold.ttf",
                "/font/Roboto/Roboto-Italic.ttf",
                "/font/Roboto/Roboto-BoldItalic.ttf"
        );
        for (String res : fontResources) {
            URL resourceUrl = PerfinApp.class.getResource(res);
            if (resourceUrl == null) {
                log.warn("Font resource {} was not found.", res);
            } else {
                Font font = Font.loadFont(PerfinApp.class.getResource(res).toExternalForm(), 10);
                if (font == null) {
                    log.warn("Failed to load font {}.", res);
                } else {
                    log.debug("Loaded font: Family = {}, Name = {}, Style = {}.", font.getFamily(), font.getName(), font.getStyle());
                }
            }
        }
    }
}