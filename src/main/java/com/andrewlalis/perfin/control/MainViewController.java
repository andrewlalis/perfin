package com.andrewlalis.perfin.control;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class MainViewController {
    @FXML
    public BorderPane mainContainer;
    @FXML
    public HBox mainFooter;

    @FXML
    public void goToAccounts() {
        router.navigate("accounts");
    }

    @FXML
    public void goToEditAccounts() {
        router.navigate("edit-account");
    }

    @FXML
    public void goBack() {
        router.navigateBack();
    }

    @FXML
    public void goForward() {
        router.navigateForward();
    }
}
