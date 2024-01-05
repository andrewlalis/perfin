package com.andrewlalis.perfin.view;

import com.andrewlalis.perfin.data.util.ThrowableConsumer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.function.Consumer;

/**
 * A splash screen that is shown as the application starts up, and does some
 * tasks before the main application can start.
 */
public class StartupSplashScreen extends Stage implements Consumer<String> {
    private final List<ThrowableConsumer<Consumer<String>>> tasks;
    private boolean startupSuccessful = false;

    private final TextArea textArea = new TextArea();

    public StartupSplashScreen(List<ThrowableConsumer<Consumer<String>>> tasks) {
        this.tasks = tasks;
        setTitle("Starting Perfin...");
        setResizable(false);
        initStyle(StageStyle.UNDECORATED);
        getIcons().add(ImageCache.getLogo64());

        setScene(buildScene());
        setOnShowing(event -> runTasks());
    }

    public boolean isStartupSuccessful() {
        return startupSuccessful;
    }

    @Override
    public void accept(String message) {
        Platform.runLater(() -> textArea.appendText(message + "\n"));
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane(textArea);
        root.setId("sceneRoot");
        root.setPrefWidth(400.0);
        root.setPrefHeight(200.0);

        textArea.setId("content");
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setFocusTraversable(false);

        Scene scene = new Scene(root, 400.0, 200.0);
        scene.getStylesheets().addAll(
                StartupSplashScreen.class.getResource("/style/base.css").toExternalForm(),
                StartupSplashScreen.class.getResource("/style/startup-splash-screen.css").toExternalForm()
        );
        return scene;
    }

    private void runTasks() {
        Thread.ofVirtual().start(() -> {
            for (var task : tasks) {
                try {
                    task.accept(this);
                    Thread.sleep(100);
                } catch (Exception e) {
                    accept("Startup failed: " + e.getMessage());
                    e.printStackTrace(System.err);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    Platform.runLater(this::close);
                    return;
                }
            }
            accept("Startup successful!");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            startupSuccessful = true;
            Platform.runLater(this::close);
        });
    }
}
