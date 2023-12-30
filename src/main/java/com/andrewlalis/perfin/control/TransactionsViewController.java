package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.Pair;
import com.andrewlalis.perfin.SceneUtil;
import com.andrewlalis.perfin.view.component.DataSourcePaginationControls;
import com.andrewlalis.perfin.view.component.TransactionTile;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class TransactionsViewController implements RouteSelectionListener {
    @FXML public BorderPane transactionsListBorderPane;
    @FXML public VBox transactionsVBox;
    private DataSourcePaginationControls paginationControls;


    @FXML public VBox detailPanel;
    private final ObjectProperty<Transaction> selectedTransaction = new SimpleObjectProperty<>(null);

    @FXML public void initialize() {
        // Initialize the left-hand paginated transactions list.
        this.paginationControls = new DataSourcePaginationControls(
                transactionsVBox.getChildren(),
                new DataSourcePaginationControls.PageFetcherFunction() {
                    @Override
                    public Page<? extends Node> fetchPage(PageRequest pagination) throws Exception {
                        try (var repo = Profile.getCurrent().getDataSource().getTransactionRepository()) {
                            return repo.findAll(pagination).map(TransactionsViewController.this::makeTile);
                        }
                    }

                    @Override
                    public int getTotalCount() throws Exception {
                        try (var repo = Profile.getCurrent().getDataSource().getTransactionRepository()) {
                            return (int) repo.countAll();
                        }
                    }
                }
        );
        transactionsListBorderPane.setBottom(paginationControls);

        // Initialize the right-hand transaction detail view.
        HBox container = (HBox) detailPanel.getParent();
        ObservableValue<Double> halfWidthProp = container.widthProperty().map(n -> n.doubleValue() * 0.5);
        detailPanel.minWidthProperty().bind(halfWidthProp);
        detailPanel.maxWidthProperty().bind(halfWidthProp);
        detailPanel.prefWidthProperty().bind(halfWidthProp);
        detailPanel.managedProperty().bind(detailPanel.visibleProperty());
        detailPanel.visibleProperty().bind(selectedTransaction.isNotNull());

        Pair<BorderPane, TransactionViewController> detailComponents = SceneUtil.loadNodeAndController("/transaction-view.fxml");
        TransactionViewController transactionViewController = detailComponents.second();
        BorderPane transactionDetailView = detailComponents.first();
        transactionDetailView.managedProperty().bind(transactionDetailView.visibleProperty());
        transactionDetailView.visibleProperty().bind(selectedTransaction.isNotNull());
        detailPanel.getChildren().add(transactionDetailView);
        selectedTransaction.addListener((observable, oldValue, newValue) -> {
            transactionViewController.setTransaction(newValue);
        });
    }

    @Override
    public void onRouteSelected(Object context) {
        paginationControls.sorts.setAll(Sort.desc("timestamp"));
        this.paginationControls.setPage(1);
    }

    @FXML
    public void addTransaction() {
        router.navigate("create-transaction");
    }

    private TransactionTile makeTile(Transaction transaction) {
        var tile = new TransactionTile(transaction);
        tile.setOnMouseClicked(event -> {
            if (selectedTransaction.get() == null || selectedTransaction.get().getId() != transaction.getId()) {
                selectedTransaction.set(transaction);
            } else {
                selectedTransaction.set(null);
            }
        });
        tile.selected.bind(selectedTransaction.map(t -> t != null && t.getId() == transaction.getId()));
        return tile;
    }

//    private void refreshTransactions() {
//        Thread.ofVirtual().start(() -> {
//            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
//                var page = repo.findAll(PageRequest.unpaged(Sort.desc("timestamp")));
//                var components = page.items().stream().map(transaction -> {
//                    var tile = new TransactionTile(transaction, this::refreshTransactions);
//                    tile.setOnMouseClicked(event -> {
//                        if (selectedTransaction.get() == null || selectedTransaction.get().getId() != transaction.getId()) {
//                            selectedTransaction.set(transaction);
//                        } else {
//                            selectedTransaction.set(null);
//                        }
//                    });
//                    tile.selected.bind(selectedTransaction.map(t -> t != null && t.getId() == transaction.getId()));
//                    return tile;
//                }).toList();
//                Platform.runLater(() -> transactionsVBox.getChildren().setAll(components));
//            });
//        });
//    }
}
