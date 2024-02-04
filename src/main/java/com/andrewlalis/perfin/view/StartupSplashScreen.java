package com.andrewlalis.perfin.view;

import com.andrewlalis.perfin.data.util.ThrowableConsumer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A splash screen that is shown as the application starts up, and does some
 * tasks before the main application can start.
 */
public class StartupSplashScreen extends Stage implements Consumer<String> {
    private final List<ThrowableConsumer<Consumer<String>>> tasks;
    private final boolean delayTasks;
    private boolean startupSuccessful = false;

    private final TextArea textArea = new TextArea();

    public StartupSplashScreen(List<ThrowableConsumer<Consumer<String>>> tasks, boolean delayTasks) {
        this.tasks = tasks;
        this.delayTasks = delayTasks;
        setTitle("Starting Perfin...");
        setResizable(false);
        initStyle(StageStyle.UNDECORATED);
        getIcons().add(ImageCache.getLogo256());

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

    /**
     * Runs all tasks sequentially, invoking each one on the JavaFX main thread,
     * and quitting if there's any exception thrown.
     */
    private void runTasks() {
        Thread.ofVirtual().start(() -> {
            if (delayTasks) sleepOrThrowRE(1000);
            for (var task : tasks) {
                try {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    Platform.runLater(() -> {
                        try {
                            task.accept(this);
                            future.complete(null);
                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });
                    future.join();
                    if (delayTasks) sleepOrThrowRE(500);
                } catch (Exception e) {
                    accept("Startup failed: " + e.getMessage());
                    e.printStackTrace(System.err);
                    sleepOrThrowRE(5000);
                    Platform.runLater(this::close);
                    return;
                }
            }
            accept("Startup successful!");
            if (delayTasks) sleepOrThrowRE(1000);
            startupSuccessful = true;
            Platform.runLater(this::close);
        });
    }

    /**
     * Helper method to sleep the current thread or throw a runtime exception.
     * @param ms The number of milliseconds to sleep for.
     */
    private static void sleepOrThrowRE(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
