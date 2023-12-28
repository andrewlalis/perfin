package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.view.AccountComboBoxCellFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateTransactionController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public Label timestampInvalidLabel;
    @FXML public Label timestampFutureLabel;

    @FXML public TextField amountField;
    @FXML public ChoiceBox<Currency> currencyChoiceBox;
    @FXML public TextArea descriptionField;
    @FXML public Label descriptionErrorLabel;

    @FXML public ComboBox<Account> linkDebitAccountComboBox;
    @FXML public ComboBox<Account> linkCreditAccountComboBox;
    @FXML public Label linkedAccountsErrorLabel;

    @FXML public void initialize() {
        // Setup error field validation.
        timestampInvalidLabel.managedProperty().bind(timestampInvalidLabel.visibleProperty());
        timestampFutureLabel.managedProperty().bind(timestampFutureLabel.visibleProperty());
        timestampField.textProperty().addListener((observable, oldValue, newValue) -> {
            LocalDateTime parsedTimestamp = parseTimestamp();
            timestampInvalidLabel.setVisible(parsedTimestamp == null);
            timestampFutureLabel.setVisible(parsedTimestamp != null && parsedTimestamp.isAfter(LocalDateTime.now()));
        });
        descriptionErrorLabel.managedProperty().bind(descriptionErrorLabel.visibleProperty());
        descriptionErrorLabel.visibleProperty().bind(descriptionErrorLabel.textProperty().isNotEmpty());
        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 255) {
                descriptionErrorLabel.setText("Description is too long.");
            } else {
                descriptionErrorLabel.setText(null);
            }
        });
        linkedAccountsErrorLabel.managedProperty().bind(linkedAccountsErrorLabel.visibleProperty());
        linkedAccountsErrorLabel.visibleProperty().bind(linkedAccountsErrorLabel.textProperty().isNotEmpty());
        linkDebitAccountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> onLinkedAccountsUpdated());
        linkCreditAccountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> onLinkedAccountsUpdated());


        // Update the lists of accounts available for linking based on the selected currency.
        var cellFactory = new AccountComboBoxCellFactory();
        linkDebitAccountComboBox.setCellFactory(cellFactory);
        linkDebitAccountComboBox.setButtonCell(cellFactory.call(null));
        linkCreditAccountComboBox.setCellFactory(cellFactory);
        linkCreditAccountComboBox.setButtonCell(cellFactory.call(null));
        currencyChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateLinkAccountComboBoxes(newValue);
        });
    }

    @FXML public void save() {
        var validationMessages = validateFormData();
        if (!validationMessages.isEmpty()) {
            Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    "There are some issues with your data:\n\n" +
                            validationMessages.stream()
                                    .map(s -> "- " + s)
                                    .collect(Collectors.joining("\n\n"))
            );
            alert.show();
        } else {
            LocalDateTime timestamp = parseTimestamp();
            BigDecimal amount = new BigDecimal(amountField.getText());
            Currency currency = currencyChoiceBox.getValue();
            String description = descriptionField.getText() == null ? null : descriptionField.getText().strip();
            Map<Long, AccountEntry.Type> affectedAccounts = getSelectedAccounts();
            Transaction transaction = new Transaction(timestamp, amount, currency, description);
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                repo.insert(transaction, affectedAccounts);
            });
            router.navigateBackAndClear();
        }
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
        amountField.setText("0");
        descriptionField.setText(null);
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

    private Map<Long, AccountEntry.Type> getSelectedAccounts() {
        Account debitAccount = linkDebitAccountComboBox.getValue();
        Account creditAccount = linkCreditAccountComboBox.getValue();
        Map<Long, AccountEntry.Type> accountsMap = new HashMap<>();
        if (debitAccount != null) accountsMap.put(debitAccount.getId(), AccountEntry.Type.DEBIT);
        if (creditAccount != null) accountsMap.put(creditAccount.getId(), AccountEntry.Type.CREDIT);
        return accountsMap;
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

    private void onLinkedAccountsUpdated() {
        Account debitAccount = linkDebitAccountComboBox.getValue();
        Account creditAccount = linkCreditAccountComboBox.getValue();
        if (debitAccount == null && creditAccount == null) {
            linkedAccountsErrorLabel.setText("At least one credit or debit account must be linked to the transaction for it to have any effect.");
        } else if (debitAccount != null && creditAccount != null && debitAccount.getId() == creditAccount.getId()) {
            linkedAccountsErrorLabel.setText("Cannot link the same account to both credit and debit.");
        } else {
            linkedAccountsErrorLabel.setText(null);
        }
    }

    private List<String> validateFormData() {
        List<String> errorMessages = new ArrayList<>();
        if (parseTimestamp() == null) errorMessages.add("Invalid or missing timestamp.");
        if (descriptionField.getText() != null && descriptionField.getText().strip().length() > 255) {
            errorMessages.add("Description is too long.");
        }
        try {
            BigDecimal value = new BigDecimal(amountField.getText());
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errorMessages.add("Amount should be a positive number.");
            }
        } catch (NumberFormatException e) {
            errorMessages.add("Invalid or missing amount.");
        }
        Account debitAccount = linkDebitAccountComboBox.getValue();
        Account creditAccount = linkCreditAccountComboBox.getValue();
        if (debitAccount == null && creditAccount == null) {
            errorMessages.add("At least one account must be linked to this transaction.");
        }
        if (debitAccount != null && creditAccount != null && debitAccount.getId() == creditAccount.getId()) {
            errorMessages.add("Credit and debit accounts cannot be the same.");
        }
        return errorMessages;
    }
}
