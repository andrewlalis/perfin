package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.PropertiesPane;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.CurrencyAmountValidator;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditAccountController implements RouteSelectionListener {
    private static final Logger log = LoggerFactory.getLogger(EditAccountController.class);

    private Account account;
    private final BooleanProperty creatingNewAccount = new SimpleBooleanProperty(false);

    @FXML public Label titleLabel;
    @FXML public TextField accountNameField;
    @FXML public TextField accountNumberField;
    @FXML public ComboBox<Currency> accountCurrencyComboBox;
    @FXML public ChoiceBox<AccountType> accountTypeChoiceBox;
    @FXML public TextArea descriptionField;
    @FXML public PropertiesPane initialBalanceContent;
    @FXML public TextField initialBalanceField;

    @FXML public Button saveButton;

    @FXML
    public void initialize() {
        var nameValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s != null && !s.isBlank(), "Name should not be empty.")
                .addPredicate(s -> s.strip().length() <= 63, "Name is too long.")
        ).attachToTextField(accountNameField);

        var numberValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s != null && !s.isBlank(), "Account number should not be empty.")
                .addPredicate(s -> s.strip().length() <= 255, "Account number is too long.")
        ).attachToTextField(accountNumberField);

        var balanceValid = new ValidationApplier<>(
                new CurrencyAmountValidator(() -> accountCurrencyComboBox.getValue(), true, false)
        ).attachToTextField(initialBalanceField, accountCurrencyComboBox.valueProperty());

        var descriptionValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addPredicate(s -> s == null || s.strip().length() <= Account.DESCRIPTION_MAX_LENGTH, "Description is too long.")
        ).attach(descriptionField, descriptionField.textProperty());

        // Combine validity of all fields for an expression that determines if the whole form is valid.
        BooleanExpression formValid = nameValid.and(numberValid).and(balanceValid.or(creatingNewAccount.not())).and(descriptionValid);
        saveButton.disableProperty().bind(formValid.not());

        List<Currency> priorityCurrencies = Stream.of("USD", "EUR", "GBP", "CAD", "AUD")
                .map(Currency::getInstance)
                .toList();
        List<Currency> availableCurrencies = Currency.getAvailableCurrencies().stream()
                .filter(c -> !priorityCurrencies.contains(c))
                .sorted(Comparator.comparing(Currency::getCurrencyCode))
                .toList();
        List<Currency> allCurrencies = new ArrayList<>();
        allCurrencies.addAll(priorityCurrencies);
        allCurrencies.addAll(availableCurrencies);
        accountCurrencyComboBox.getItems().addAll(allCurrencies);
        accountCurrencyComboBox.getSelectionModel().selectFirst();

        accountTypeChoiceBox.getItems().add(AccountType.CHECKING);
        accountTypeChoiceBox.getItems().add(AccountType.SAVINGS);
        accountTypeChoiceBox.getItems().add(AccountType.CREDIT_CARD);
        accountTypeChoiceBox.getItems().add(AccountType.BROKERAGE);
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
        String name = accountNameField.getText().strip();
        String number = accountNumberField.getText().strip();
        AccountType type = accountTypeChoiceBox.getValue();
        Currency currency = accountCurrencyComboBox.getValue();
        String description = descriptionField.getText();
        if (description != null) {
            description = description.strip();
            if (description.isBlank()) description = null;
        }
        try (
                var accountRepo = Profile.getCurrent().dataSource().getAccountRepository();
                var balanceRepo = Profile.getCurrent().dataSource().getBalanceRecordRepository()
        ) {
            if (creatingNewAccount.get()) {
                BigDecimal initialBalance = new BigDecimal(initialBalanceField.getText().strip());
                List<Path> attachments = Collections.emptyList();

                String prompt = String.format(
                        "Are you sure you want to create this account?\nName: %s\nNumber: %s\nType: %s\nInitial Balance: %s",
                        name,
                        number,
                        type.toString(),
                        CurrencyUtil.formatMoneyWithCurrencyPrefix(new MoneyValue(initialBalance, currency))
                );
                boolean success = Popups.confirm(accountNameField, prompt);
                if (success) {
                    long id = accountRepo.insert(type, number, name, currency, description);
                    balanceRepo.insert(LocalDateTime.now(ZoneOffset.UTC), id, initialBalance, currency, attachments);
                    // Once we create the new account, go to the account.
                    Account newAccount = accountRepo.findById(id).orElseThrow();
                    router.replace("account", newAccount);
                }
            } else {
                accountRepo.update(account.id, type, number, name, currency, description);
                Account updatedAccount = accountRepo.findById(account.id).orElseThrow();
                router.replace("account", updatedAccount);
            }
        } catch (Exception e) {
            log.error("Failed to save (or update) account " + account.id, e);
            Popups.error(accountNameField, "Failed to save the account: " + e.getMessage());
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
            descriptionField.setText(null);
        } else {
            accountNameField.setText(account.getName());
            accountNumberField.setText(account.getAccountNumber());
            accountTypeChoiceBox.getSelectionModel().select(account.getType());
            accountCurrencyComboBox.getSelectionModel().select(account.getCurrency());
            descriptionField.setText(account.getDescription());
        }
    }
}
