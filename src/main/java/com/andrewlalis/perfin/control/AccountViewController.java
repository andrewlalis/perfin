package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import com.andrewlalis.perfin.view.component.AccountHistoryItemTile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountViewController implements RouteSelectionListener {
    private Account account;

    @FXML public Label titleLabel;
    @FXML public TextField accountNameField;
    @FXML public TextField accountNumberField;
    @FXML public TextField accountCreatedAtField;
    @FXML public TextField accountCurrencyField;
    @FXML public TextField accountBalanceField;

    @FXML public VBox historyItemsVBox;
    @FXML public Button loadMoreHistoryButton;
    private LocalDateTime loadHistoryFrom;
    private final int historyLoadSize = 5;

    @Override
    public void onRouteSelected(Object context) {
        account = (Account) context;
        titleLabel.setText("Account #" + account.getId());

        accountNameField.setText(account.getName());
        accountNumberField.setText(account.getAccountNumber());
        accountCurrencyField.setText(account.getCurrency().getDisplayName());
        accountCreatedAtField.setText(DateUtil.formatUTCAsLocalWithZone(account.getCreatedAt()));
        Profile.getCurrent().getDataSource().getAccountBalanceText(account, accountBalanceField::setText);

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
        var confirmResult = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to archive this account? It will no " +
                        "longer show up in the app normally, and you won't be " +
                        "able to add new transactions to it. You'll still be " +
                        "able to view the account, and you can un-archive it " +
                        "later if you need to."
        ).showAndWait();
        if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> repo.archive(account));
            router.getHistory().clear();
            router.navigate("accounts");
        }
    }

    @FXML
    public void deleteAccount() {
        var confirmResult = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to permanently delete this account and " +
                        "all data directly associated with it? This cannot be " +
                        "undone; deleted accounts are not recoverable at all. " +
                        "Consider archiving this account instead if you just " +
                        "want to hide it."
        ).showAndWait();
        if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> repo.delete(account));
            router.getHistory().clear();
            router.navigate("accounts");
        }
    }

    @FXML public void loadMoreHistory() {
        Thread.ofVirtual().start(() -> {
            try (var historyRepo = Profile.getCurrent().getDataSource().getAccountHistoryItemRepository()) {
                List<AccountHistoryItem> historyItems = historyRepo.findMostRecentForAccount(
                        account.getId(),
                        loadHistoryFrom,
                        historyLoadSize
                );
                if (historyItems.size() < historyLoadSize) {
                    Platform.runLater(() -> loadMoreHistoryButton.setDisable(true));
                } else {
                    loadHistoryFrom = historyItems.getLast().getTimestamp();
                }
                List<? extends Node> nodes = historyItems.stream()
                        .map(item -> AccountHistoryItemTile.forItem(item, historyRepo, this))
                        .toList();
                Platform.runLater(() -> historyItemsVBox.getChildren().addAll(nodes));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
