package com.andrewlalis.perfin.view;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * A stage that shows a popup for interacting with Perfin's collection of
 * profiles.
 */
public class ProfilesStage extends Stage {
    private static ProfilesStage instance;

    public ProfilesStage() {
        setTitle("Profiles");
        setAlwaysOnTop(false);
        initModality(Modality.APPLICATION_MODAL);
        setScene(SceneUtil.load("/profiles-view.fxml"));
    }

    public static void open(Window owner) {
        if (instance == null) {
            instance = new ProfilesStage();
            instance.initOwner(owner);
            instance.show();
            instance.setOnCloseRequest(event -> instance = null);
        } else {
            instance.requestFocus();
            instance.toFront();
        }
    }

    public static void closeView() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
