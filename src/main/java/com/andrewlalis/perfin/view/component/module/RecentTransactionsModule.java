package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.control.TransactionsViewController;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.StyledText;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.andrewlalis.perfin.PerfinApp.router;

public class RecentTransactionsModule extends DashboardModule {
    private final VBox transactionsVBox = new VBox();

    public RecentTransactionsModule(Pane parent) {
        super(parent);
        transactionsVBox.getStyleClass().add("tile-container");
        ScrollPane scrollPane = new ScrollPane(transactionsVBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addTransactionButton = new Button("Add Transaction");
        addTransactionButton.setOnAction(event -> router.navigate("edit-transaction"));
        Button viewTransactionsButton = new Button("All Transactions");
        viewTransactionsButton.setOnAction(event -> router.navigate("transactions"));
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshContents());

        this.getChildren().add(new ModuleHeader(
                "Recent Transactions",
                addTransactionButton,
                viewTransactionsButton,
                refreshButton
        ));
        this.getChildren().add(scrollPane);
    }

    @Override
    public void refreshContents() {
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionRepository.class,
                repo -> repo.findRecentN(5)
        )
            .thenApply(transactions -> transactions.stream().map(RecentTransactionsModule::buildMiniTransactionTile).toList())
            .thenAccept(nodes -> Platform.runLater(() -> {
                transactionsVBox.getChildren().clear();
                transactionsVBox.getChildren().addAll(nodes);
            }));
    }

    private static Node buildMiniTransactionTile(Transaction tx) {
        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().addAll("tile", "hand-cursor");
        borderPane.setOnMouseClicked(event -> router.navigate("transactions", new TransactionsViewController.RouteContext(tx.id)));

        Label dateLabel = new Label(DateUtil.formatUTCAsLocalWithZone(tx.getTimestamp()));
        dateLabel.getStyleClass().addAll("mono-font", "small-font", "secondary-color-text-fill");
        StyledText linkedAccountsLabel = new StyledText();
        linkedAccountsLabel.getStyleClass().addAll("small-font");
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionRepository.class,
                repo -> repo.findLinkedAccounts(tx.id)
        )
            .thenAccept(accounts -> Platform.runLater(() -> {
                StringBuilder sb = new StringBuilder();
                if (accounts.hasCredit()) {
                    sb.append("Credited from **").append(accounts.creditAccount().getName()).append("**");
                    if (accounts.hasDebit()) sb.append(". ");
                }
                if (accounts.hasDebit()) {
                    sb.append("Debited to **").append(accounts.debitAccount().getName()).append("**");
                }
                linkedAccountsLabel.setText(sb.toString());
            }));
        Label descriptionLabel = new Label(tx.getDescription());
        BindingUtil.bindManagedAndVisible(descriptionLabel, descriptionLabel.textProperty().isNotEmpty());

        Label balanceLabel = new Label(CurrencyUtil.formatMoneyWithCurrencyPrefix(tx.getMoneyAmount()));
        balanceLabel.getStyleClass().addAll("mono-font");

        VBox contentBox = new VBox(dateLabel, descriptionLabel, linkedAccountsLabel);
        borderPane.setCenter(contentBox);
        borderPane.setRight(balanceLabel);

        return borderPane;
    }
}
