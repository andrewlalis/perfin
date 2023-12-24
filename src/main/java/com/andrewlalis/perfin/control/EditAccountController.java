package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Currency;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditAccountController implements RouteSelectionListener {
    private Account account;
    @FXML
    public Label titleLabel;
    @FXML
    public TextField accountNameField;
    @FXML
    public TextField accountNumberField;
    @FXML
    public ComboBox<Currency> accountCurrencyComboBox;
    @FXML
    public ChoiceBox<String> accountTypeChoiceBox;

    @FXML
    public void initialize() {
        final String[] currencies = {
                "USD",
                "EUR",
                "GBP"
        };
        for (String currencyCode : currencies) {
            accountCurrencyComboBox.getItems().add(Currency.getInstance(currencyCode));
        }
        accountCurrencyComboBox.getSelectionModel().select(Currency.getInstance("USD"));

        accountTypeChoiceBox.getItems().add("Checking");
        accountTypeChoiceBox.getItems().add("Savings");
        accountTypeChoiceBox.getItems().add("Credit Card");
        accountTypeChoiceBox.getSelectionModel().select("Checking");
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        if (account == null) {
            titleLabel.setText("Editing New Account");
        } else {
            titleLabel.setText("Editing Account: " + account.getName());
        }
        resetForm();
    }

    @FXML
    public void save() {
        if (account == null) {
            // If we're editing a new account.
            String name = accountNameField.getText().strip();
            String number = accountNumberField.getText().strip();
            AccountType type = AccountType.parse(accountTypeChoiceBox.getValue());
            Currency currency = accountCurrencyComboBox.getValue();
            Account newAccount = new Account(type, number, name, currency);
            Profile.getCurrent().getDataSource().getAccountRepository().insert(newAccount);

            // Once we create the new account, go to the account.
            router.getHistory().clear();
            router.navigate("accounts");
        } else {
            throw new IllegalStateException("Not implemented.");
        }
    }

    @FXML
    public void cancel() {
        router.navigateBack();
    }

    public void resetForm() {
        if (account == null) {
            accountNameField.setText("");
            accountNumberField.setText("");
            accountTypeChoiceBox.getSelectionModel().selectFirst();
            accountCurrencyComboBox.getSelectionModel().select(Currency.getInstance("USD"));
        } else {
            // TODO: Set to original account.
        }
    }
}
