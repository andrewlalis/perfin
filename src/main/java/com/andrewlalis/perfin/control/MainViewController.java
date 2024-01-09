package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.ProfilesStage;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.helpRouter;
import static com.andrewlalis.perfin.PerfinApp.router;

public class MainViewController {
    @FXML public BorderPane mainContainer;
    @FXML public HBox breadcrumbHBox;

    @FXML public Button showManualButton;
    @FXML public Button hideManualButton;
    @FXML public VBox manualVBox;

    @FXML public void initialize() {
        AnchorPaneRouterView routerView = (AnchorPaneRouterView) router.getView();
        mainContainer.setCenter(routerView.getAnchorPane());

        // Set up a simple breadcrumb display in the top bar.
        BindingUtil.mapContent(
                breadcrumbHBox.getChildren(),
                router.getBreadCrumbs(),
                breadCrumb -> {
                    Label label = new Label("> " + breadCrumb.route());
                    if (breadCrumb.current()) {
                        label.getStyleClass().add("bold-text");
                    }
                    return label;
                }
        );

        router.navigate("accounts");

        // Initialize the help manual components.
        manualVBox.managedProperty().bind(manualVBox.visibleProperty());
        manualVBox.setVisible(false);
        showManualButton.managedProperty().bind(showManualButton.visibleProperty());
        showManualButton.visibleProperty().bind(manualVBox.visibleProperty().not());
        hideManualButton.managedProperty().bind(hideManualButton.visibleProperty());
        hideManualButton.visibleProperty().bind(manualVBox.visibleProperty());

        AnchorPaneRouterView helpRouterView = (AnchorPaneRouterView) helpRouter.getView();
        manualVBox.getChildren().add(helpRouterView.getAnchorPane());

        helpRouter.navigate("home");
    }

    @FXML public void goBack() {
        router.navigateBack();
    }

    @FXML public void goForward() {
        router.navigateForward();
    }

    @FXML public void goToAccounts() {
        router.getHistory().clear();
        router.navigate("accounts");
    }

    @FXML public void goToTransactions() {
        router.getHistory().clear();
        router.navigate("transactions");
    }

    @FXML public void viewProfiles() {
        ProfilesStage.open(mainContainer.getScene().getWindow());
    }

    @FXML public void showManual() {
        manualVBox.setVisible(true);
    }

    @FXML public void hideManual() {
        manualVBox.setVisible(false);
    }
}
