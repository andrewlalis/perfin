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
    public ChoiceBox<AccountType> accountTypeChoiceBox;

    private boolean editingNewAccount() {
        return account == null;
    }

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

        accountTypeChoiceBox.getItems().add(AccountType.CHECKING);
        accountTypeChoiceBox.getItems().add(AccountType.SAVINGS);
        accountTypeChoiceBox.getItems().add(AccountType.CREDIT_CARD);
        accountTypeChoiceBox.getSelectionModel().select(AccountType.CHECKING);
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        if (editingNewAccount()) {
            titleLabel.setText("Editing New Account");
        } else {
            titleLabel.setText("Editing Account: " + account.getName());
        }
        resetForm();
    }

    @FXML
    public void save() {
        try (var accountRepo = Profile.getCurrent().getDataSource().getAccountRepository()) {
            if (editingNewAccount()) {
                String name = accountNameField.getText().strip();
                String number = accountNumberField.getText().strip();
                AccountType type = accountTypeChoiceBox.getValue();
                Currency currency = accountCurrencyComboBox.getValue();
                Account newAccount = new Account(type, number, name, currency);
                long id = accountRepo.insert(newAccount);
                Account savedAccount = accountRepo.findById(id).orElseThrow();

                // Once we create the new account, go to the account.
                router.getHistory().clear();
                router.navigate("account", savedAccount);
            } else {
                System.out.println("Updating account " + account.getName());
                account.setName(accountNameField.getText().strip());
                account.setAccountNumber(accountNumberField.getText().strip());
                account.setType(accountTypeChoiceBox.getValue());
                account.setCurrency(accountCurrencyComboBox.getValue());
                accountRepo.update(account);
                Account updatedAccount = accountRepo.findById(account.getId()).orElseThrow();
                router.getHistory().clear();
                router.navigate("account", updatedAccount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void cancel() {
        router.navigateBackAndClear();
    }

    public void resetForm() {
        if (account == null) {
            accountNameField.setText("");
            accountNumberField.setText("");
            accountTypeChoiceBox.getSelectionModel().selectFirst();
            accountCurrencyComboBox.getSelectionModel().select(Currency.getInstance("USD"));
        } else {
            accountNameField.setText(account.getName());
            accountNumberField.setText(account.getAccountNumber());
            accountTypeChoiceBox.getSelectionModel().select(account.getType());
            accountCurrencyComboBox.getSelectionModel().select(account.getCurrency());
        }
    }
}
