package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.Profile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditAccountController implements RouteSelectionListener {
    private Account account;
    private final BooleanProperty creatingNewAccount = new SimpleBooleanProperty(false);

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
    @FXML
    public VBox initialBalanceContent;
    @FXML
    public TextField initialBalanceField;

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

        initialBalanceContent.visibleProperty().bind(creatingNewAccount);
        initialBalanceContent.managedProperty().bind(creatingNewAccount);
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        creatingNewAccount.set(account == null);
        if (creatingNewAccount.get()) {
            titleLabel.setText("Editing New Account");
        } else {
            titleLabel.setText("Editing Account: " + account.getName());
        }
        resetForm();
    }

    @FXML
    public void save() {
        try (
                var accountRepo = Profile.getCurrent().getDataSource().getAccountRepository();
                var balanceRepo = Profile.getCurrent().getDataSource().getBalanceRecordRepository()
        ) {
            if (creatingNewAccount.get()) {
                String name = accountNameField.getText().strip();
                String number = accountNumberField.getText().strip();
                AccountType type = accountTypeChoiceBox.getValue();
                Currency currency = accountCurrencyComboBox.getValue();
                BigDecimal initialBalance = new BigDecimal(initialBalanceField.getText().strip());

                Alert confirm = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to create this account?"
                );
                Optional<ButtonType> result = confirm.showAndWait();
                boolean success = result.isPresent() && result.get().equals(ButtonType.OK);
                if (success) {
                    Account newAccount = new Account(type, number, name, currency);
                    long id = accountRepo.insert(newAccount);
                    Account savedAccount = accountRepo.findById(id).orElseThrow();
                    balanceRepo.insert(new BalanceRecord(id, initialBalance, savedAccount.getCurrency()));

                    // Once we create the new account, go to the account.
                    router.getHistory().clear();
                    router.navigate("account", savedAccount);
                }
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
        if (creatingNewAccount.get()) {
            accountNameField.setText("");
            accountNumberField.setText("");
            accountTypeChoiceBox.getSelectionModel().selectFirst();
            accountCurrencyComboBox.getSelectionModel().select(Currency.getInstance("USD"));
            initialBalanceField.setText(String.format("%.02f", 0f));
        } else {
            accountNameField.setText(account.getName());
            accountNumberField.setText(account.getAccountNumber());
            accountTypeChoiceBox.getSelectionModel().select(account.getType());
            accountCurrencyComboBox.getSelectionModel().select(account.getCurrency());
        }
    }
}
