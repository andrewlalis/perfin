package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.AnchorPaneRouterView;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class MainViewController {
    @FXML
    public BorderPane mainContainer;
    @FXML
    public HBox mainFooter;
    @FXML
    public HBox breadcrumbHBox;

    @FXML
    public void initialize() {
        AnchorPaneRouterView routerView = (AnchorPaneRouterView) router.getView();
        mainContainer.setCenter(routerView.getAnchorPane());
        routerView.getAnchorPane().setStyle("-fx-border-color: orange;");
        // Set up a simple breadcrumb display in the top bar.
        BindingUtil.mapContent(
                breadcrumbHBox.getChildren(),
                router.getBreadCrumbs(),
                breadCrumb -> {
                    Label label = new Label("> " + breadCrumb.route());
                    if (breadCrumb.current()) {
                        label.setStyle("-fx-font-weight: bold");
                    }
                    return label;
                }
        );

        router.navigate("accounts");
    }

    @FXML
    public void goToAccounts() {
        router.navigate("accounts");
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
