package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.data.FileUtil;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.ProfilesStage;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class ProfilesViewController {
    @FXML public VBox profilesVBox;
    @FXML public TextField newProfileNameField;
    @FXML public Text newProfileNameErrorLabel;
    @FXML public Button addProfileButton;

    @FXML public void initialize() {
        BooleanExpression newProfileNameValid = BooleanProperty.booleanExpression(newProfileNameField.textProperty()
                .map(text -> (
                        text != null &&
                        !text.isBlank() &&
                        Profile.validateName(text) &&
                        !Profile.getAvailableProfiles().contains(text)
                )));
        newProfileNameErrorLabel.managedProperty().bind(newProfileNameErrorLabel.visibleProperty());
        newProfileNameErrorLabel.visibleProperty().bind(newProfileNameValid.not().and(newProfileNameField.textProperty().isNotEmpty()));
        newProfileNameErrorLabel.wrappingWidthProperty().bind(newProfileNameField.widthProperty());
        addProfileButton.disableProperty().bind(newProfileNameValid.not());

        refreshAvailableProfiles();
    }

    @FXML public void addProfile() {
        String name = newProfileNameField.getText();
        boolean valid = Profile.validateName(name);
        if (valid && !Profile.getAvailableProfiles().contains(name)) {
            boolean confirm = Popups.confirm("Are you sure you want to add a new profile named \"" + name + "\"?");
            if (confirm) {
                if (openProfile(name, false)) {
                    Popups.message("Created new profile \"" + name + "\" and loaded it.");
                }
                newProfileNameField.clear();
            }
        }
    }

    private void refreshAvailableProfiles() {
        List<String> profileNames = Profile.getAvailableProfiles();
        String currentProfile = Profile.getCurrent() == null ? null : Profile.getCurrent().getName();
        List<Node> nodes = new ArrayList<>(profileNames.size());
        for (String profileName : profileNames) {
            boolean isCurrent = profileName.equals(currentProfile);
            AnchorPane profilePane = new AnchorPane();
            profilePane.setStyle("""
                    -fx-border-color: lightgray;
                    -fx-border-radius: 5px;
                    -fx-padding: 5px;
                    """);

            Text nameTextElement = new Text(profileName);
            nameTextElement.setStyle("-fx-font-size: large;");
            TextFlow nameLabel = new TextFlow(nameTextElement);
            if (isCurrent) {
                nameTextElement.setStyle("-fx-font-size: large; -fx-font-weight: bold;");
                Text currentProfileIndicator = new Text(" Currently Selected Profile");
                currentProfileIndicator.setStyle("""
                        -fx-font-size: small;
                        -fx-fill: grey;
                        """);
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
            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> deleteProfile(profileName));
            buttonBox.getChildren().add(deleteButton);

            profilePane.getChildren().setAll(nameLabel, buttonBox);
            nodes.add(profilePane);
        }
        profilesVBox.getChildren().setAll(nodes);
    }

    private boolean openProfile(String name, boolean showPopup) {
        System.out.println("Opening profile: " + name);
        try {
            Profile.load(name);
            ProfilesStage.closeView();
            router.getHistory().clear();
            router.navigate("accounts");
            if (showPopup) Popups.message("The profile \"" + name + "\" has been loaded.");
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            Popups.error("Failed to load profile: " + e.getMessage());
            return false;
        }
    }

    private void deleteProfile(String name) {
        boolean confirmA = Popups.confirm("Are you sure you want to delete the profile \"" + name + "\"? This will permanently delete ALL accounts, transactions, files, and other data for this profile, and it cannot be recovered.");
        if (confirmA) {
            boolean confirmB = Popups.confirm("Press \"OK\" to confirm that you really want to delete the profile \"" + name + "\". There's no going back.");
            if (confirmB) {
                try {
                    FileUtil.deleteDirRecursive(Profile.getDir(name));
                    // Reset the app's "last profile" to the default if it was the deleted profile.
                    if (Profile.getLastProfile().equals(name)) {
                        Profile.saveLastProfile("default");
                    }
                    // If the current profile was deleted, switch to the default.
                    if (Profile.getCurrent() != null && Profile.getCurrent().getName().equals(name)) {
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
