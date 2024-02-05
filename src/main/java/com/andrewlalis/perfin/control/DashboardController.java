package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.view.component.module.*;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;

public class DashboardController implements RouteSelectionListener {
    @FXML public ScrollPane modulesScrollPane;
    @FXML public FlowPane modulesFlowPane;

    @FXML public void initialize() {
        var viewportWidth = modulesScrollPane.viewportBoundsProperty().map(Bounds::getWidth);
        modulesFlowPane.minWidthProperty().bind(viewportWidth);
        modulesFlowPane.prefWidthProperty().bind(viewportWidth);
        modulesFlowPane.maxWidthProperty().bind(viewportWidth);

        var accountsModule = new AccountsModule(modulesFlowPane);
        accountsModule.columnsProperty.set(2);

        var transactionsModule = new RecentTransactionsModule(modulesFlowPane);
        transactionsModule.columnsProperty.set(2);

        var m3 = new SpendingCategoryChartModule(modulesFlowPane);
        m3.columnsProperty.set(2);

        var m4 = new VendorSpendChartModule(modulesFlowPane);
        m4.columnsProperty.set(2);

        modulesFlowPane.getChildren().addAll(accountsModule, transactionsModule, m3, m4);
    }

    @Override
    public void onRouteSelected(Object context) {
        for (var child : modulesFlowPane.getChildren()) {
            DashboardModule module = (DashboardModule) child;
            module.refreshContents();
        }
    }
}
