package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

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

    @Override
    public void onRouteSelected(Object context) {
        account = (Account) context;
        titleLabel.setText("Account: " + account.getAccountNumber());

        accountNameField.setText(account.getName());
        accountNumberField.setText(account.getAccountNumber());
        accountCurrencyField.setText(account.getCurrency().getDisplayName());
        accountCreatedAtField.setText(account.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @FXML
    public void goToEditPage() {
        router.navigate("edit-account", account);
    }

    @FXML
    public void deleteAccount() {
        Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
            repo.delete(account);
        });
        router.getHistory().clear();
        router.navigate("accounts");
    }
}
