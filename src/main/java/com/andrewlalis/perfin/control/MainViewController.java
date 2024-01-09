package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.ProfilesStage;
import com.andrewlalis.perfin.view.component.ScrollPaneRouterView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import static com.andrewlalis.perfin.PerfinApp.helpRouter;
import static com.andrewlalis.perfin.PerfinApp.router;

public class MainViewController {
    @FXML public BorderPane mainContainer;
    @FXML public HBox breadcrumbHBox;

    @FXML public Button showManualButton;
    @FXML public Button hideManualButton;
    @FXML public BorderPane helpPane;
    @FXML public Button helpBackButton;

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
        helpPane.managedProperty().bind(helpPane.visibleProperty());
        helpPane.setVisible(false);
        showManualButton.managedProperty().bind(showManualButton.visibleProperty());
        showManualButton.visibleProperty().bind(helpPane.visibleProperty().not());
        hideManualButton.managedProperty().bind(hideManualButton.visibleProperty());
        hideManualButton.visibleProperty().bind(helpPane.visibleProperty());

        helpBackButton.managedProperty().bind(helpBackButton.visibleProperty());
        helpRouter.currentRouteProperty().addListener((observable, oldValue, newValue) -> {
            helpBackButton.setVisible(helpRouter.getHistory().canGoBack());
        });
        helpBackButton.setOnAction(event -> helpRouter.navigateBack());

        ScrollPaneRouterView helpRouterView = (ScrollPaneRouterView) helpRouter.getView();
        ScrollPane helpRouterScrollPane = helpRouterView.getScrollPane();
        helpRouterScrollPane.setMinWidth(200.0);
        helpRouterScrollPane.setMaxWidth(400.0);
        helpRouterScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        helpRouterScrollPane.getStyleClass().addAll("padding-extra");
        helpPane.setCenter(helpRouterScrollPane);

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
        helpPane.setVisible(true);
    }

    @FXML public void hideManual() {
        helpPane.setVisible(false);
    }

    @FXML public void helpViewHome() {
        helpRouter.getHistory().clear();
        helpRouter.navigate("home");
    }

    @FXML public void helpViewAccounts() {
        helpRouter.getHistory().clear();
        helpRouter.navigate("accounts");
    }

    @FXML public void helpViewTransactions() {
        helpRouter.getHistory().clear();
        helpRouter.navigate("transactions");
    }
}
