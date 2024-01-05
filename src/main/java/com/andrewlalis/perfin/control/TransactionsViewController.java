package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.data.util.Pair;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.view.AccountComboBoxCellFactory;
import com.andrewlalis.perfin.view.SceneUtil;
import com.andrewlalis.perfin.view.component.DataSourcePaginationControls;
import com.andrewlalis.perfin.view.component.TransactionTile;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Set;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * Controller for the view of all transactions in a user's profile.
 * Transactions are displayed in a paginated manner, and this controller
 * accepts as a route context a {@link PageRequest} to initialize the results
 * to a specific page.
 */
public class TransactionsViewController implements RouteSelectionListener {
    public static List<Sort> DEFAULT_SORTS = List.of(Sort.desc("timestamp"));
    public static int DEFAULT_ITEMS_PER_PAGE = 5;
    public record RouteContext(Long selectedTransactionId) {}

    @FXML public BorderPane transactionsListBorderPane;
    @FXML public ComboBox<Account> filterByAccountComboBox;
    @FXML public VBox transactionsVBox;
    private DataSourcePaginationControls paginationControls;


    @FXML public VBox detailPanel;
    private final ObjectProperty<Transaction> selectedTransaction = new SimpleObjectProperty<>(null);

    @FXML public void initialize() {
        // Initialize the left-hand paginated transactions list.
        var accountCellFactory = new AccountComboBoxCellFactory("All");
        filterByAccountComboBox.setCellFactory(accountCellFactory);
        filterByAccountComboBox.setButtonCell(accountCellFactory.call(null));
        filterByAccountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            paginationControls.setPage(1);
            selectedTransaction.set(null);
        });

        this.paginationControls = new DataSourcePaginationControls(
                transactionsVBox.getChildren(),
                new DataSourcePaginationControls.PageFetcherFunction() {
                    @Override
                    public Page<? extends Node> fetchPage(PageRequest pagination) throws Exception {
                        Account accountFilter = filterByAccountComboBox.getValue();
                        try (var repo = Profile.getCurrent().getDataSource().getTransactionRepository()) {
                            Page<Transaction> result;
                            if (accountFilter == null) {
                                result = repo.findAll(pagination);
                            } else {
                                result = repo.findAllByAccounts(Set.of(accountFilter.id), pagination);
                            }
                            return result.map(TransactionsViewController.this::makeTile);
                        }
                    }

                    @Override
                    public int getTotalCount() throws Exception {
                        Account accountFilter = filterByAccountComboBox.getValue();
                        try (var repo = Profile.getCurrent().getDataSource().getTransactionRepository()) {
                            if (accountFilter == null) {
                                return (int) repo.countAll();
                            } else {
                                return (int) repo.countAllByAccounts(Set.of(accountFilter.id));
                            }
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

        // Clear the transactions when a new profile is loaded.
        Profile.whenLoaded(profile -> {
            transactionsVBox.getChildren().clear();
            onRouteSelected(null);
        });
    }

    @Override
    public void onRouteSelected(Object context) {
        paginationControls.sorts.setAll(DEFAULT_SORTS);
        paginationControls.itemsPerPage.set(DEFAULT_ITEMS_PER_PAGE);

        // Refresh account filter options.
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                List<Account> accounts = repo.findAll(PageRequest.unpaged(Sort.asc("name"))).items();
                accounts.add(null);
                Platform.runLater(() -> {
                    filterByAccountComboBox.getItems().clear();
                    filterByAccountComboBox.getItems().addAll(accounts);
                    filterByAccountComboBox.getSelectionModel().selectLast();
                    filterByAccountComboBox.getButtonCell().updateIndex(accounts.size() - 1);
                });
            });
        });


        // If a transaction id is given in the route context, navigate to the page it's on and select it.
        if (context instanceof RouteContext ctx && ctx.selectedTransactionId != null) {
            Thread.ofVirtual().start(() -> {
                Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                    repo.findById(ctx.selectedTransactionId).ifPresent(tx -> {
                        long offset = repo.countAllAfter(tx.id);
                        int pageNumber = (int) (offset / DEFAULT_ITEMS_PER_PAGE) + 1;
                        paginationControls.setPage(pageNumber).thenRun(() -> selectedTransaction.set(tx));
                    });
                });
            });
        } else {
            paginationControls.setPage(1);
            selectedTransaction.set(null);
        }
    }

    @FXML
    public void addTransaction() {
        router.navigate("create-transaction");
    }

    private TransactionTile makeTile(Transaction transaction) {
        var tile = new TransactionTile(transaction);
        tile.setOnMouseClicked(event -> {
            if (selectedTransaction.get() == null || !selectedTransaction.get().equals(transaction)) {
                selectedTransaction.set(transaction);
            } else {
                selectedTransaction.set(null);
            }
        });
        tile.selected.bind(selectedTransaction.map(t -> t != null && t.equals(transaction)));
        return tile;
    }
}
