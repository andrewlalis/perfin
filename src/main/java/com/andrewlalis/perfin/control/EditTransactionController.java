package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.view.component.AccountSelectionBox;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditTransactionController implements RouteSelectionListener {
    private static final Logger log = LoggerFactory.getLogger(EditTransactionController.class);

    @FXML public Label titleLabel;

    @FXML public TextField timestampField;
    @FXML public TextField amountField;
    @FXML public ChoiceBox<Currency> currencyChoiceBox;
    @FXML public TextArea descriptionField;

    @FXML public HBox linkedAccountsContainer;
    @FXML public AccountSelectionBox debitAccountSelector;
    @FXML public AccountSelectionBox creditAccountSelector;

    @FXML public VBox attachmentsVBox;
    private FileSelectionArea attachmentsSelectionArea;

    @FXML public Button saveButton;

    private Transaction transaction;

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

        // Linked accounts will use a property derived from both the debit and credit selections.
        Property<CreditAndDebitAccounts> linkedAccountsProperty = new SimpleObjectProperty<>(getSelectedAccounts());
        debitAccountSelector.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        creditAccountSelector.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        var linkedAccountsValid = new ValidationApplier<>(new PredicateValidator<CreditAndDebitAccounts>()
                .addPredicate(accounts -> accounts.hasCredit() || accounts.hasDebit(), "At least one account must be linked.")
                .addPredicate(
                        accounts -> (!accounts.hasCredit() || !accounts.hasDebit()) || !accounts.creditAccount().equals(accounts.debitAccount()),
                        "The credit and debit accounts cannot be the same."
                )
                .addPredicate(
                        accounts -> (
                                (!accounts.hasCredit() || accounts.creditAccount().getCurrency().equals(currencyChoiceBox.getValue())) ||
                                (!accounts.hasDebit() || accounts.debitAccount().getCurrency().equals(currencyChoiceBox.getValue()))
                        ),
                        "Linked accounts must use the same currency."
                )
        ).validatedInitially().attach(linkedAccountsContainer, linkedAccountsProperty);

        var formValid = timestampValid.and(amountValid).and(descriptionValid).and(linkedAccountsValid);
        saveButton.disableProperty().bind(formValid.not());

        // Initialize the file selection area.
        attachmentsSelectionArea = new FileSelectionArea(
                FileUtil::newAttachmentsFileChooser,
                () -> attachmentsVBox.getScene().getWindow()
        );
        attachmentsSelectionArea.allowMultiple.set(true);
        attachmentsVBox.getChildren().add(attachmentsSelectionArea);
    }

    @FXML public void save() {
        final long idToNavigate;
        if (transaction == null) {
            LocalDateTime utcTimestamp = DateUtil.localToUTC(parseTimestamp());
            BigDecimal amount = new BigDecimal(amountField.getText());
            Currency currency = currencyChoiceBox.getValue();
            String description = getSanitizedDescription();
            CreditAndDebitAccounts linkedAccounts = getSelectedAccounts();
            List<Path> attachments = attachmentsSelectionArea.getSelectedFiles();
            Profile.getCurrent().getDataSource().useRepo(TransactionRepository.class, repo -> {
                repo.insert(
                        utcTimestamp,
                        amount,
                        currency,
                        description,
                        linkedAccounts,
                        attachments
                );
            });
        }
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }

    @Override
    public void onRouteSelected(Object context) {
        transaction = (Transaction) context;
        boolean creatingNew = transaction == null;

        if (creatingNew) {
            titleLabel.setText("Create New Transaction");
            timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
            amountField.setText(null);
            descriptionField.setText(null);
            attachmentsSelectionArea.clear();
        } else {
            titleLabel.setText("Edit Transaction #" + transaction.id);
            timestampField.setText(DateUtil.formatUTCAsLocal(transaction.getTimestamp()));
            amountField.setText(CurrencyUtil.formatMoneyAsBasicNumber(transaction.getMoneyAmount()));
            descriptionField.setText(transaction.getDescription());

            // TODO: Add an editable list of attachments from which some can be added and removed.

        }

        // Fetch some account-specific data.
        currencyChoiceBox.setDisable(true);
        creditAccountSelector.setDisable(true);
        debitAccountSelector.setDisable(true);
        Thread.ofVirtual().start(() -> {
            try (
                    var accountRepo = Profile.getCurrent().getDataSource().getAccountRepository();
                    var transactionRepo = Profile.getCurrent().getDataSource().getTransactionRepository()
            ) {
                var currencies = accountRepo.findAllUsedCurrencies().stream()
                        .sorted(Comparator.comparing(Currency::getCurrencyCode))
                        .toList();
                var accounts = accountRepo.findAll(PageRequest.unpaged(Sort.asc("name"))).items();
                CreditAndDebitAccounts linkedAccounts = transaction == null ? null : transactionRepo.findLinkedAccounts(transaction.id);
                Platform.runLater(() -> {
                    creditAccountSelector.setAccounts(accounts);
                    debitAccountSelector.setAccounts(accounts);
                    currencyChoiceBox.getItems().setAll(currencies);
                    if (creatingNew) {
                        // TODO: Allow user to select a default currency.
                        currencyChoiceBox.getSelectionModel().selectFirst();
                        creditAccountSelector.select(null);
                        debitAccountSelector.select(null);
                    } else {
                        currencyChoiceBox.getSelectionModel().select(transaction.getCurrency());
                        if (linkedAccounts != null) {
                            creditAccountSelector.select(linkedAccounts.creditAccount());
                            debitAccountSelector.select(linkedAccounts.debitAccount());
                        }
                    }
                    currencyChoiceBox.setDisable(false);
                    creditAccountSelector.setDisable(false);
                    debitAccountSelector.setDisable(false);
                });
            } catch (Exception e) {
                log.error("Failed to get repositories.", e);
                Popups.error("Failed to fetch account-specific data: " + e.getMessage());
            }
        });
    }

    private CreditAndDebitAccounts getSelectedAccounts() {
        return new CreditAndDebitAccounts(
                creditAccountSelector.getValue(),
                debitAccountSelector.getValue()
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

    private String getSanitizedDescription() {
        String raw = descriptionField.getText();
        if (raw == null) return null;
        if (raw.isBlank()) return null;
        return raw.strip();
    }
}
