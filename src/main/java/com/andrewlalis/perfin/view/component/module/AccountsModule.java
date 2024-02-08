package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.AccountTile;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.math.BigDecimal;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A module that displays a basic overview of recent accounts.
 */
public class AccountsModule extends DashboardModule {
    private final VBox accountsVBox = new VBox();
    private final Label totalAssetsLabel = new Label();

    public AccountsModule(Pane parent) {
        super(parent);
        accountsVBox.getStyleClass().add("tile-container");
        ScrollPane scrollPane = new ScrollPane(accountsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addAccountButton = new Button("Add Account");
        addAccountButton.setOnAction(event -> router.navigate("edit-account", null));
        Button viewAllAccountsButton = new Button("All Accounts");
        viewAllAccountsButton.setOnAction(event -> router.navigate("accounts"));
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshContents());

        this.getChildren().add(new ModuleHeader(
                "Recently Active Accounts",
                addAccountButton,
                viewAllAccountsButton,
                refreshButton
        ));
        this.getChildren().add(scrollPane);

        HBox footer = new HBox();
        footer.getStyleClass().addAll("std-padding", "std-spacing");

        totalAssetsLabel.getStyleClass().addAll("mono-font");
        HBox totalAssetsBox = new HBox(new Label("Total Tracked Assets: "), totalAssetsLabel);
        totalAssetsBox.getStyleClass().addAll("std-spacing");
        footer.getChildren().add(totalAssetsBox);

        this.getChildren().add(footer);
    }

    @Override
    public void refreshContents() {
        Profile.getCurrent().dataSource().mapRepoAsync(
            AccountRepository.class,
            AccountRepository::findAllOrderedByRecentHistory
        )
            .thenApply(accounts -> accounts.stream().map(AccountsModule::buildMiniAccountTile).toList())
            .thenAccept(nodes -> Platform.runLater(() -> {
                accountsVBox.getChildren().clear();
                accountsVBox.getChildren().addAll(nodes);
            }));
        totalAssetsLabel.setText("Computing...");
        totalAssetsLabel.setDisable(true);
        Profile.getCurrent().dataSource().getCombinedAccountBalances()
                .thenApply(CurrencyUtil::formatMoneyValues)
                .thenAccept(s -> Platform.runLater(() -> {
                    totalAssetsLabel.setText(s);
                    totalAssetsLabel.setDisable(false);
                }));
    }

    private static Node buildMiniAccountTile(Account account) {
        BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().addAll("tile", "hand-cursor");
        borderPane.setOnMouseClicked(event -> router.navigate("account", account));

        Label nameLabel = new Label(account.getName());
        nameLabel.getStyleClass().addAll("bold-text");
        Label numberLabel = new Label(account.getAccountNumberSuffix());
        numberLabel.getStyleClass().addAll("mono-font");
        Label typeLabel = new Label(account.getType().toString());
        typeLabel.getStyleClass().add("bold-text");
        typeLabel.setStyle("-fx-text-fill: " + AccountTile.ACCOUNT_TYPE_COLORS.get(account.getType()));
        Label balanceLabel = new Label("Computing balance...");
        balanceLabel.getStyleClass().addAll("mono-font");
        balanceLabel.setDisable(true);

        Profile.getCurrent().dataSource().mapRepoAsync(
                AccountRepository.class,
                repo -> repo.deriveCurrentBalance(account.id)
        ).thenAccept(bal -> Platform.runLater(() -> {
            String text = CurrencyUtil.formatMoneyWithCurrencyPrefix(new MoneyValue(bal, account.getCurrency()));
            balanceLabel.setText(text);
            if (account.getType().areDebitsPositive() && bal.compareTo(BigDecimal.ZERO) < 0) {
                balanceLabel.getStyleClass().add("negative-color-text-fill");
            } else if (!account.getType().areDebitsPositive() && bal.compareTo(BigDecimal.ZERO) < 0) {
                balanceLabel.getStyleClass().add("positive-color-text-fill");
            }
            balanceLabel.setDisable(false);
        }));

        VBox contentBox = new VBox(nameLabel, numberLabel, typeLabel);
        borderPane.setCenter(contentBox);
        borderPane.setRight(balanceLabel);
        return borderPane;
    }
}
