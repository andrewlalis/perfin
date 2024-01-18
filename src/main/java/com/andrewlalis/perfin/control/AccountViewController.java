package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import com.andrewlalis.perfin.view.component.AccountHistoryItemTile;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountViewController implements RouteSelectionListener {
    private Account account;

    @FXML public Label titleLabel;

    @FXML public Label accountNameLabel;
    @FXML public Label accountNumberLabel;
    @FXML public Label accountCurrencyLabel;
    @FXML public Label accountCreatedAtLabel;
    @FXML public Label accountBalanceLabel;
    @FXML public BooleanProperty accountArchivedProperty = new SimpleBooleanProperty(false);

    @FXML public VBox historyItemsVBox;
    @FXML public Button loadMoreHistoryButton;
    private LocalDateTime loadHistoryFrom;
    private final int historyLoadSize = 5;

    @FXML public VBox actionsVBox;

    @FXML public void initialize() {
        actionsVBox.getChildren().forEach(node -> {
            Button button = (Button) node;
            BooleanExpression buttonActive = accountArchivedProperty;
            if (button.getText().equalsIgnoreCase("Unarchive")) {
                buttonActive = buttonActive.not();
            }
            button.disableProperty().bind(buttonActive);
            button.managedProperty().bind(button.visibleProperty());
            button.visibleProperty().bind(button.disableProperty().not());
        });
    }

    @Override
    public void onRouteSelected(Object context) {
        account = (Account) context;
        accountArchivedProperty.set(account.isArchived());
        titleLabel.setText("Account #" + account.id);
        accountNameLabel.setText(account.getName());
        accountNumberLabel.setText(account.getAccountNumber());
        accountCurrencyLabel.setText(account.getCurrency().getDisplayName());
        accountCreatedAtLabel.setText(DateUtil.formatUTCAsLocalWithZone(account.getCreatedAt()));
        Profile.getCurrent().dataSource().getAccountBalanceText(account)
                .thenAccept(accountBalanceLabel::setText);

        reloadHistory();
    }

    public void reloadHistory() {
        loadHistoryFrom = DateUtil.nowAsUTC();
        historyItemsVBox.getChildren().clear();
        loadMoreHistoryButton.setDisable(false);
        loadMoreHistory();
    }

    @FXML
    public void goToEditPage() {
        router.navigate("edit-account", account);
    }

    @FXML public void goToCreateBalanceRecord() {
        router.navigate("create-balance-record", account);
    }

    @FXML
    public void archiveAccount() {
        boolean confirmResult = Popups.confirm(
                titleLabel,
                "Are you sure you want to archive this account? It will no " +
                        "longer show up in the app normally, and you won't be " +
                        "able to add new transactions to it. You'll still be " +
                        "able to view the account, and you can un-archive it " +
                        "later if you need to."
        );
        if (confirmResult) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.archive(account.id));
            router.replace("accounts");
        }
    }

    @FXML public void unarchiveAccount() {
        boolean confirm = Popups.confirm(
                titleLabel,
                "Are you sure you want to restore this account from its archived " +
                        "status?"
        );
        if (confirm) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.unarchive(account.id));
            router.replace("accounts");
        }
    }

    @FXML
    public void deleteAccount() {
        boolean confirm = Popups.confirm(
                titleLabel,
                "Are you sure you want to permanently delete this account and " +
                        "all data directly associated with it? This cannot be " +
                        "undone; deleted accounts are not recoverable at all. " +
                        "Consider archiving this account instead if you just " +
                        "want to hide it."
        );
        if (confirm) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.delete(account));
            router.replace("accounts");
        }
    }

    @FXML public void loadMoreHistory() {
        Profile.getCurrent().dataSource().useRepoAsync(AccountHistoryItemRepository.class, repo -> {
            List<AccountHistoryItem> historyItems = repo.findMostRecentForAccount(
                    account.id,
                    loadHistoryFrom,
                    historyLoadSize
            );
            if (historyItems.size() < historyLoadSize) {
                Platform.runLater(() -> loadMoreHistoryButton.setDisable(true));
            } else {
                loadHistoryFrom = historyItems.getLast().getTimestamp();
            }
            List<? extends Node> nodes = historyItems.stream()
                    .map(item -> AccountHistoryItemTile.forItem(item, repo, this))
                    .toList();
            Platform.runLater(() -> historyItemsVBox.getChildren().addAll(nodes));
        });
    }
}
