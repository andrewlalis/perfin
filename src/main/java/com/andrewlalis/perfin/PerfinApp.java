package com.andrewlalis.perfin;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.javafx_scene_router.SceneRouter;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.impl.JdbcDataSourceFactory;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.ProfileLoader;
import com.andrewlalis.perfin.view.ImageCache;
import com.andrewlalis.perfin.view.SceneUtil;
import com.andrewlalis.perfin.view.StartupSplashScreen;
import com.andrewlalis.perfin.view.component.ScrollPaneRouterView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static ProfileLoader profileLoader;

    /**
     * The router that's used for navigating between different "pages" in the application.
     */
    public static final SceneRouter router = new SceneRouter(new AnchorPaneRouterView(true));

    /**
     * A router that controls which help page is being viewed in the side-pane.
     * Certain user actions may cause this router to navigate to certain pages.
     */
    public static final SceneRouter helpRouter = new SceneRouter(new ScrollPaneRouterView());

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        profileLoader = new ProfileLoader(stage, new JdbcDataSourceFactory());
        loadFonts();
        var splashScreen = new StartupSplashScreen(List.of(
                PerfinApp::defineRoutes,
                PerfinApp::initAppDir,
                c -> initMainScreen(stage, c),
                PerfinApp::loadLastUsedProfile
        ), false);
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
            mainViewScene.getStylesheets().addAll(
                    PerfinApp.class.getResource("/style/base.css").toExternalForm()
            );
            stage.setScene(mainViewScene);
            stage.setTitle("Perfin");
            stage.getIcons().add(ImageCache.getLogo256());
        });
    }

    private static void defineRoutes(Consumer<String> msgConsumer) {
        msgConsumer.accept("Initializing application views.");
        Platform.runLater(() -> {
            // App pages.
            router.map("accounts", PerfinApp.class.getResource("/accounts-view.fxml"));
            router.map("account", PerfinApp.class.getResource("/account-view.fxml"));
            router.map("edit-account", PerfinApp.class.getResource("/edit-account.fxml"));
            router.map("transactions", PerfinApp.class.getResource("/transactions-view.fxml"));
            router.map("edit-transaction", PerfinApp.class.getResource("/edit-transaction.fxml"));
            router.map("create-balance-record", PerfinApp.class.getResource("/create-balance-record.fxml"));
            router.map("balance-record", PerfinApp.class.getResource("/balance-record-view.fxml"));
            router.map("vendors", PerfinApp.class.getResource("/vendors-view.fxml"));
            router.map("edit-vendor", PerfinApp.class.getResource("/edit-vendor.fxml"));
            router.map("categories", PerfinApp.class.getResource("/categories-view.fxml"));
            router.map("edit-category", PerfinApp.class.getResource("/edit-category.fxml"));
            router.map("tags", PerfinApp.class.getResource("/tags-view.fxml"));

            // Help pages.
            helpRouter.map("home", PerfinApp.class.getResource("/help-pages/home.fxml"));
            helpRouter.map("accounts", PerfinApp.class.getResource("/help-pages/accounts-view.fxml"));
            helpRouter.map("adding-an-account", PerfinApp.class.getResource("/help-pages/adding-an-account.fxml"));
            helpRouter.map("transactions", PerfinApp.class.getResource("/help-pages/transactions-view.fxml"));
            helpRouter.map("adding-a-transaction", PerfinApp.class.getResource("/help-pages/adding-a-transaction.fxml"));
            helpRouter.map("profiles", PerfinApp.class.getResource("/help-pages/profiles.fxml"));
            helpRouter.map("about", PerfinApp.class.getResource("/help-pages/about.fxml"));
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
        String lastProfile = ProfileLoader.getLastProfile();
        msgConsumer.accept("Loading the most recent profile: \"" + lastProfile + "\".");
        try {
            Profile.setCurrent(profileLoader.load(lastProfile));
        } catch (ProfileLoadException e) {
            msgConsumer.accept("Failed to load the profile: " + e.getMessage());
            throw e;
        }
    }

    private static void loadFonts() {
        List<String> fontResources = List.of(
                "/font/JetBrainsMono-2.304/fonts/ttf/JetBrainsMono-Regular.ttf",
                "/font/JetBrainsMono-2.304/fonts/ttf/JetBrainsMono-Bold.ttf",
                "/font/JetBrainsMono-2.304/fonts/ttf/JetBrainsMono-Italic.ttf",
                "/font/JetBrainsMono-2.304/fonts/ttf/JetBrainsMono-BoldItalic.ttf",
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
                Font font = Font.loadFont(resourceUrl.toExternalForm(), 10);
                if (font == null) {
                    log.warn("Failed to load font {}.", res);
                } else {
                    log.trace("Loaded font: Family = {}, Name = {}, Style = {}.", font.getFamily(), font.getName(), font.getStyle());
                }
            }
        }
    }
}