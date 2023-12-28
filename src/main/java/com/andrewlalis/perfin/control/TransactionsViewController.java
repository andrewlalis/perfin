package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.control.component.TransactionTile;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.model.Profile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class TransactionsViewController implements RouteSelectionListener {
    @FXML
    public VBox transactionsVBox;

    @Override
    public void onRouteSelected(Object context) {
        refreshTransactions();
    }

    @FXML
    public void addTransaction() {
        router.navigate("create-transaction");
    }

    private void refreshTransactions() {
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                var page = repo.findAll(PageRequest.unpaged(Sort.desc("timestamp")));
                var components = page.items().stream().map(transaction -> new TransactionTile(transaction, this::refreshTransactions)).toList();
                Platform.runLater(() -> transactionsVBox.getChildren().setAll(components));
            });
        });
    }
}
