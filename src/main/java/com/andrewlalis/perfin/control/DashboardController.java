package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.view.component.module.AccountsModule;
import com.andrewlalis.perfin.view.component.module.DashboardModule;
import com.andrewlalis.perfin.view.component.module.RecentTransactionsModule;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;

public class DashboardController implements RouteSelectionListener {
    @FXML public ScrollPane modulesScrollPane;
    @FXML public FlowPane modulesFlowPane;

    private DashboardModule accountsModule;
    private DashboardModule transactionsModule;

    @FXML public void initialize() {
        var viewportWidth = modulesScrollPane.viewportBoundsProperty().map(Bounds::getWidth);
        modulesFlowPane.minWidthProperty().bind(viewportWidth);
        modulesFlowPane.prefWidthProperty().bind(viewportWidth);
        modulesFlowPane.maxWidthProperty().bind(viewportWidth);

        accountsModule = new AccountsModule(modulesFlowPane);
        accountsModule.columnsProperty.set(2);

        transactionsModule = new RecentTransactionsModule(modulesFlowPane);
        transactionsModule.columnsProperty.set(2);

        modulesFlowPane.getChildren().addAll(accountsModule, transactionsModule);
    }

    @Override
    public void onRouteSelected(Object context) {
        accountsModule.refreshContents();
        transactionsModule.refreshContents();
    }
}
