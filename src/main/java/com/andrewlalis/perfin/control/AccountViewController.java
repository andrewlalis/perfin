package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountViewController implements RouteSelectionListener {
    private Account account;

    @FXML
    public Label titleLabel;
    @FXML
    public TextField accountNameField;
    @FXML
    public TextField accountNumberField;
    @FXML
    public TextField accountCreatedAtField;
    @FXML
    public TextField accountCurrencyField;
    @FXML
    public TextField accountBalanceField;

    @Override
    public void onRouteSelected(Object context) {
        account = (Account) context;
        titleLabel.setText("Account: " + account.getAccountNumber());

        accountNameField.setText(account.getName());
        accountNumberField.setText(account.getAccountNumber());
        accountCurrencyField.setText(account.getCurrency().getDisplayName());
        accountCreatedAtField.setText(DateUtil.formatUTCAsLocalWithZone(account.getCreatedAt()));
        Profile.getCurrent().getDataSource().getAccountBalanceText(account, accountBalanceField::setText);
    }

    @FXML
    public void goToEditPage() {
        router.navigate("edit-account", account);
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
}
