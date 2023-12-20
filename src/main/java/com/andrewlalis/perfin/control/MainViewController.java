package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.view.SceneRouter;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class MainViewController {
    public SceneRouter router;
    @FXML
    public BorderPane mainContainer;
    @FXML
    public HBox mainFooter;

    @FXML
    public void goToAccounts() {
        router.goTo("accounts");
    }

    @FXML
    public void goToEditAccounts() {
        router.goTo("edit-account");
    }
}
