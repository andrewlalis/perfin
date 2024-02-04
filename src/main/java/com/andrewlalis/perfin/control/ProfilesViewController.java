package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.PerfinApp;
import com.andrewlalis.perfin.data.ProfileLoadException;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.ProfileLoader;
import com.andrewlalis.perfin.view.ProfilesStage;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class ProfilesViewController {
    private static final Logger log = LoggerFactory.getLogger(ProfilesViewController.class);

    @FXML public VBox profilesVBox;
    @FXML public TextField newProfileNameField;
    @FXML public Button addProfileButton;

    @FXML public void initialize() {
        var newProfileNameValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addPredicate(s -> s == null || s.isBlank() || Profile.validateName(s), "Profile name should consist of only lowercase numbers.")
        ).attachToTextField(newProfileNameField);
        addProfileButton.disableProperty().bind(newProfileNameValid.not());

        refreshAvailableProfiles();
    }

    @FXML public void addProfile() {
        String name = newProfileNameField.getText();
        boolean valid = Profile.validateName(name);
        if (valid && !ProfileLoader.getAvailableProfiles().contains(name)) {
            boolean confirm = Popups.confirm(profilesVBox, "Are you sure you want to add a new profile named \"" + name + "\"?");
            if (confirm) {
                if (openProfile(name, false)) {
                    Popups.message(profilesVBox, "Created new profile \"" + name + "\" and loaded it.");
                }
                newProfileNameField.clear();
            }
        }
    }

    private void refreshAvailableProfiles() {
        List<String> profileNames = ProfileLoader.getAvailableProfiles();
        String currentProfile = Profile.getCurrent() == null ? null : Profile.getCurrent().name();
        List<Node> nodes = new ArrayList<>(profileNames.size());
        for (String profileName : profileNames) {
            boolean isCurrent = profileName.equals(currentProfile);
            AnchorPane profilePane = new AnchorPane();
            profilePane.getStyleClass().add("tile");

            Text nameTextElement = new Text(profileName);
            nameTextElement.getStyleClass().add("large-font");
            TextFlow nameLabel = new TextFlow(nameTextElement);
            if (isCurrent) {
                nameTextElement.getStyleClass().addAll("large-font", "bold-text");
                Text currentProfileIndicator = new Text(" Currently Selected Profile");
                currentProfileIndicator.getStyleClass().addAll("small-font", "secondary-color-fill");
                nameLabel.getChildren().add(currentProfileIndicator);
            }
            AnchorPane.setLeftAnchor(nameLabel, 0.0);
            AnchorPane.setTopAnchor(nameLabel, 0.0);
            AnchorPane.setBottomAnchor(nameLabel, 0.0);

            HBox buttonBox = new HBox();
            AnchorPane.setRightAnchor(buttonBox, 0.0);
            AnchorPane.setTopAnchor(buttonBox, 0.0);
            AnchorPane.setBottomAnchor(buttonBox, 0.0);
            buttonBox.getStyleClass().addAll("std-spacing");
            Button openButton = new Button("Open");
            openButton.setOnAction(event -> openProfile(profileName, false));
            openButton.setDisable(isCurrent);
            buttonBox.getChildren().add(openButton);
            Button viewFilesButton = new Button("View Files");
            viewFilesButton.setOnAction(event -> {
                PerfinApp.instance.getHostServices().showDocument(Profile.getDir(profileName).toUri().toString());
            });
            buttonBox.getChildren().add(viewFilesButton);
            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> deleteProfile(profileName));
            buttonBox.getChildren().add(deleteButton);

            profilePane.getChildren().setAll(nameLabel, buttonBox);
            nodes.add(profilePane);
        }
        profilesVBox.getChildren().setAll(nodes);
    }

    private boolean openProfile(String name, boolean showPopup) {
        log.info("Opening profile \"{}\".", name);
        try {
            Profile.setCurrent(PerfinApp.profileLoader.load(name));
            ProfileLoader.saveLastProfile(name);
            ProfilesStage.closeView();
            router.replace("dashboard");
            if (showPopup) Popups.message(profilesVBox, "The profile \"" + name + "\" has been loaded.");
            return true;
        } catch (ProfileLoadException e) {
            Popups.error(profilesVBox, "Failed to load the profile: " + e.getMessage());
            return false;
        }
    }

    private void deleteProfile(String name) {
        boolean confirmA = Popups.confirm(profilesVBox, "Are you sure you want to delete the profile \"" + name + "\"? This will permanently delete ALL accounts, transactions, files, and other data for this profile, and it cannot be recovered.");
        if (confirmA) {
            boolean confirmB = Popups.confirm(profilesVBox, "Press \"OK\" to confirm that you really want to delete the profile \"" + name + "\". There's no going back.");
            if (confirmB) {
                try {
                    FileUtil.deleteDirRecursive(Profile.getDir(name));
                    // Reset the app's "last profile" to the default if it was the deleted profile.
                    if (ProfileLoader.getLastProfile().equals(name)) {
                        ProfileLoader.saveLastProfile("default");
                    }
                    // If the current profile was deleted, switch to the default.
                    if (Profile.getCurrent() != null && Profile.getCurrent().name().equals(name)) {
                        openProfile("default", true);
                    }
                    refreshAvailableProfiles();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
