package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.AccountComboBoxCellFactory;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.CurrencyAmountValidator;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateTransactionController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public TextField amountField;
    @FXML public ChoiceBox<Currency> currencyChoiceBox;
    @FXML public TextArea descriptionField;

    @FXML public HBox linkedAccountsContainer;
    @FXML public ComboBox<Account> linkDebitAccountComboBox;
    @FXML public ComboBox<Account> linkCreditAccountComboBox;

    @FXML public VBox attachmentsVBox;
    private FileSelectionArea attachmentsSelectionArea;

    @FXML public Button saveButton;

    public CreateTransactionController() {
    }

    @FXML public void initialize() {
        // Setup error field validation.
        var timestampValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> parseTimestamp() != null, "Invalid timestamp.")
                .addPredicate(s -> {
                    LocalDateTime ts = parseTimestamp();
                    return ts != null && ts.isBefore(LocalDateTime.now());
                }, "Timestamp cannot be in the future.")
        ).validatedInitially().attachToTextField(timestampField);

        var amountValid = new ValidationApplier<>(
                new CurrencyAmountValidator(() -> currencyChoiceBox.getValue(), false, false)
        ).validatedInitially().attachToTextField(amountField, currencyChoiceBox.valueProperty());

        var descriptionValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s == null || s.length() <= 255, "Description is too long.")
        ).validatedInitially().attach(descriptionField, descriptionField.textProperty());

        Property<CreditAndDebitAccounts> linkedAccountsProperty = new SimpleObjectProperty<>(getSelectedAccounts());
        linkDebitAccountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        linkCreditAccountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        var linkedAccountsValid = new ValidationApplier<>(new PredicateValidator<CreditAndDebitAccounts>()
                .addPredicate(accounts -> accounts.hasCredit() || accounts.hasDebit(), "At least one account must be linked.")
                .addPredicate(
                        accounts -> (!accounts.hasCredit() || !accounts.hasDebit()) || !accounts.creditAccount().equals(accounts.debitAccount()),
                        "The credit and debit accounts cannot be the same."
                )
        ).validatedInitially().attach(linkedAccountsContainer, linkedAccountsProperty);

        var formValid = timestampValid.and(amountValid).and(descriptionValid).and(linkedAccountsValid);
        saveButton.disableProperty().bind(formValid.not());

        // Update the lists of accounts available for linking based on the selected currency.
        var cellFactory = new AccountComboBoxCellFactory();
        linkDebitAccountComboBox.setCellFactory(cellFactory);
        linkDebitAccountComboBox.setButtonCell(cellFactory.call(null));
        linkCreditAccountComboBox.setCellFactory(cellFactory);
        linkCreditAccountComboBox.setButtonCell(cellFactory.call(null));
        currencyChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateLinkAccountComboBoxes(newValue);
        });

        // Initialize the file selection area.
        attachmentsSelectionArea = new FileSelectionArea(
                FileUtil::newAttachmentsFileChooser,
                () -> attachmentsVBox.getScene().getWindow()
        );
        attachmentsSelectionArea.allowMultiple.set(true);
        attachmentsVBox.getChildren().add(attachmentsSelectionArea);
    }

    @FXML public void save() {
        LocalDateTime utcTimestamp = DateUtil.localToUTC(parseTimestamp());
        BigDecimal amount = new BigDecimal(amountField.getText());
        Currency currency = currencyChoiceBox.getValue();
        String description = descriptionField.getText() == null ? null : descriptionField.getText().strip();
        CreditAndDebitAccounts linkedAccounts = getSelectedAccounts();
        List<Path> attachments = attachmentsSelectionArea.getSelectedFiles();
        Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
            repo.insert(
                    utcTimestamp,
                    amount,
                    currency,
                    description,
                    linkedAccounts,
                    attachments
            );
        });
        router.navigateBackAndClear();
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }

    @Override
    public void onRouteSelected(Object context) {
        resetForm();
    }

    private void resetForm() {
        timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
        amountField.setText(null);
        descriptionField.setText(null);
        attachmentsSelectionArea.clear();
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                var currencies = repo.findAllUsedCurrencies().stream()
                        .sorted(Comparator.comparing(Currency::getCurrencyCode))
                        .toList();
                Platform.runLater(() -> {
                    currencyChoiceBox.getItems().setAll(currencies);
                    currencyChoiceBox.getSelectionModel().selectFirst();
                });
            });
        });
    }

    private CreditAndDebitAccounts getSelectedAccounts() {
        return new CreditAndDebitAccounts(
                linkCreditAccountComboBox.getValue(),
                linkDebitAccountComboBox.getValue()
        );
    }

    private LocalDateTime parseTimestamp() {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss")
        );
        for (var formatter : formatters) {
            try {
                return formatter.parse(timestampField.getText(), LocalDateTime::from);
            } catch (DateTimeException e) {
                // Ignore.
            }
        }
        return null;
    }

    private void updateLinkAccountComboBoxes(Currency currency) {
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                List<Account> availableAccounts = new ArrayList<>();
                if (currency != null) availableAccounts.addAll(repo.findAllByCurrency(currency));
                availableAccounts.add(null);
                Platform.runLater(() -> {
                    linkDebitAccountComboBox.getItems().clear();
                    linkDebitAccountComboBox.getItems().addAll(availableAccounts);
                    linkDebitAccountComboBox.getSelectionModel().selectLast();
                    linkDebitAccountComboBox.getButtonCell().updateIndex(availableAccounts.size() - 1);

                    linkCreditAccountComboBox.getItems().clear();
                    linkCreditAccountComboBox.getItems().addAll(availableAccounts);
                    linkCreditAccountComboBox.getSelectionModel().selectLast();
                    linkCreditAccountComboBox.getButtonCell().updateIndex(availableAccounts.size() - 1);
                });
            });
        });
    }
}
