package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.*;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.AccountSelectionBox;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.CurrencyAmountValidator;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.andrewlalis.perfin.PerfinApp.router;

public class EditTransactionController implements RouteSelectionListener {
    private static final Logger log = LoggerFactory.getLogger(EditTransactionController.class);

    @FXML public BorderPane container;
    @FXML public Label titleLabel;

    @FXML public TextField timestampField;
    @FXML public TextField amountField;
    @FXML public ChoiceBox<Currency> currencyChoiceBox;
    @FXML public TextArea descriptionField;

    @FXML public HBox linkedAccountsContainer;
    @FXML public AccountSelectionBox debitAccountSelector;
    @FXML public AccountSelectionBox creditAccountSelector;

    @FXML public ComboBox<String> vendorComboBox;
    @FXML public ComboBox<String> categoryComboBox;
    @FXML public ComboBox<String> tagsComboBox;
    @FXML public Button addTagButton;
    @FXML public VBox tagsVBox;
    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();

    @FXML public FileSelectionArea attachmentsSelectionArea;

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
        var linkedAccountsValid = new ValidationApplier<>(getLinkedAccountsValidator())
                .validatedInitially()
                .attach(linkedAccountsContainer, linkedAccountsProperty);

        // Set up the list of added tags.
        addTagButton.disableProperty().bind(tagsComboBox.valueProperty().map(s -> s == null || s.isBlank()));
        addTagButton.setOnAction(event -> {
            if (tagsComboBox.getValue() == null) return;
            String tag = tagsComboBox.getValue().strip();
            if (!selectedTags.contains(tag)) {
                selectedTags.add(tag);
                selectedTags.sort(String::compareToIgnoreCase);
            }
            tagsComboBox.setValue(null);
        });
        tagsComboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addTagButton.fire();
            }
        });
        BindingUtil.mapContent(tagsVBox.getChildren(), selectedTags, tag -> {
            Label label = new Label(tag);
            label.setMaxWidth(Double.POSITIVE_INFINITY);
            label.getStyleClass().addAll("bold-text");
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(event -> {
                selectedTags.remove(tag);
            });
            BorderPane tile = new BorderPane(label);
            tile.setRight(removeButton);
            tile.getStyleClass().addAll("std-spacing");
            BorderPane.setAlignment(label, Pos.CENTER_LEFT);
            return tile;
        });

        var formValid = timestampValid.and(amountValid).and(descriptionValid).and(linkedAccountsValid);
        saveButton.disableProperty().bind(formValid.not());
    }

    @FXML public void save() {
        LocalDateTime utcTimestamp = DateUtil.localToUTC(parseTimestamp());
        BigDecimal amount = new BigDecimal(amountField.getText());
        Currency currency = currencyChoiceBox.getValue();
        String description = getSanitizedDescription();
        CreditAndDebitAccounts linkedAccounts = getSelectedAccounts();
        String vendor = vendorComboBox.getValue();
        String category = categoryComboBox.getValue();
        Set<String> tags = new HashSet<>(selectedTags);
        List<Path> newAttachmentPaths = attachmentsSelectionArea.getSelectedPaths();
        List<Attachment> existingAttachments = attachmentsSelectionArea.getSelectedAttachments();
        final long idToNavigate;
        if (transaction == null) {
            idToNavigate = Profile.getCurrent().dataSource().mapRepo(
                TransactionRepository.class,
                repo -> repo.insert(
                    utcTimestamp,
                    amount,
                    currency,
                    description,
                    linkedAccounts,
                    vendor,
                    category,
                    tags,
                    newAttachmentPaths
                )
            );
        } else {
            Profile.getCurrent().dataSource().useRepo(
                TransactionRepository.class,
                repo -> repo.update(
                        transaction.id,
                        utcTimestamp,
                        amount,
                        currency,
                        description,
                        linkedAccounts,
                        vendor,
                        category,
                        tags,
                        existingAttachments,
                        newAttachmentPaths
                )
            );
            idToNavigate = transaction.id;
        }
        router.replace("transactions", new TransactionsViewController.RouteContext(idToNavigate));
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }

    @Override
    public void onRouteSelected(Object context) {
        transaction = (Transaction) context;

        // Clear some initial fields immediately:
        tagsComboBox.setValue(null);
        vendorComboBox.setValue(null);
        categoryComboBox.setValue(null);

        if (transaction == null) {
            titleLabel.setText("Create New Transaction");
            timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
            amountField.setText(null);
            descriptionField.setText(null);
        } else {
            titleLabel.setText("Edit Transaction #" + transaction.id);
            timestampField.setText(DateUtil.formatUTCAsLocal(transaction.getTimestamp()));
            amountField.setText(CurrencyUtil.formatMoneyAsBasicNumber(transaction.getMoneyAmount()));
            descriptionField.setText(transaction.getDescription());
        }

        // Fetch some account-specific data.
        container.setDisable(true);
        DataSource ds = Profile.getCurrent().dataSource();
        Thread.ofVirtual().start(() -> {
            try (
                    var accountRepo = ds.getAccountRepository();
                    var transactionRepo = ds.getTransactionRepository();
                    var vendorRepo = ds.getTransactionVendorRepository();
                    var categoryRepo = ds.getTransactionCategoryRepository()
            ) {
                // First fetch all the data.
                List<Currency> currencies = accountRepo.findAllUsedCurrencies().stream()
                        .sorted(Comparator.comparing(Currency::getCurrencyCode))
                        .toList();
                List<Account> accounts = accountRepo.findAll(PageRequest.unpaged(Sort.asc("name"))).items();
                final List<Attachment> attachments;
                final List<String> availableTags = transactionRepo.findAllTags();
                final List<String> tags;
                final CreditAndDebitAccounts linkedAccounts;
                final String vendorName;
                final String categoryName;
                if (transaction == null) {
                    attachments = Collections.emptyList();
                    tags = Collections.emptyList();
                    linkedAccounts = new CreditAndDebitAccounts(null, null);
                    vendorName = null;
                    categoryName = null;
                } else {
                    attachments = transactionRepo.findAttachments(transaction.id);
                    tags = transactionRepo.findTags(transaction.id);
                    linkedAccounts = transactionRepo.findLinkedAccounts(transaction.id);
                    if (transaction.getVendorId() != null) {
                        vendorName = vendorRepo.findById(transaction.getVendorId())
                                .map(TransactionVendor::getName).orElse(null);
                    } else {
                        vendorName = null;
                    }
                    if (transaction.getCategoryId() != null) {
                        categoryName = categoryRepo.findById(transaction.getCategoryId())
                                .map(TransactionCategory::getName).orElse(null);
                    } else {
                        categoryName = null;
                    }
                }
                final List<TransactionVendor> availableVendors = vendorRepo.findAll();
                final List<TransactionCategory> availableCategories = categoryRepo.findAll();
                // Then make updates to the view.
                Platform.runLater(() -> {
                    currencyChoiceBox.getItems().setAll(currencies);
                    creditAccountSelector.setAccounts(accounts);
                    debitAccountSelector.setAccounts(accounts);
                    vendorComboBox.getItems().setAll(availableVendors.stream().map(TransactionVendor::getName).toList());
                    vendorComboBox.setValue(vendorName);
                    categoryComboBox.getItems().setAll(availableCategories.stream().map(TransactionCategory::getName).toList());
                    categoryComboBox.setValue(categoryName);
                    tagsComboBox.getItems().setAll(availableTags);
                    attachmentsSelectionArea.clear();
                    attachmentsSelectionArea.addAttachments(attachments);
                    selectedTags.clear();
                    if (transaction == null) {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                        creditAccountSelector.select(null);
                        debitAccountSelector.select(null);
                    } else {
                        currencyChoiceBox.getSelectionModel().select(transaction.getCurrency());
                        creditAccountSelector.select(linkedAccounts.creditAccount());
                        debitAccountSelector.select(linkedAccounts.debitAccount());
                        selectedTags.addAll(tags);
                    }
                    container.setDisable(false);
                });
            } catch (Exception e) {
                log.error("Failed to get repositories.", e);
                Platform.runLater(() -> Popups.error(container, "Failed to fetch account-specific data: " + e.getMessage()));
                router.navigateBackAndClear();
            }
        });
    }

    private CreditAndDebitAccounts getSelectedAccounts() {
        return new CreditAndDebitAccounts(
                creditAccountSelector.getValue(),
                debitAccountSelector.getValue()
        );
    }

    private PredicateValidator<CreditAndDebitAccounts> getLinkedAccountsValidator() {
        return new PredicateValidator<CreditAndDebitAccounts>()
            .addPredicate(accounts -> accounts.hasCredit() || accounts.hasDebit(), "At least one account must be linked.")
            .addPredicate(
                accounts -> (!accounts.hasCredit() || !accounts.hasDebit()) || !accounts.creditAccount().equals(accounts.debitAccount()),
                "The credit and debit accounts cannot be the same."
            )
            .addPredicate(
                accounts -> (
                    (!accounts.hasCredit() || accounts.creditAccount().getCurrency().equals(currencyChoiceBox.getValue())) &&
                    (!accounts.hasDebit() || accounts.debitAccount().getCurrency().equals(currencyChoiceBox.getValue()))
                ),
                "Linked accounts must use the same currency."
            )
            .addPredicate(
                accounts -> (
                    (!accounts.hasCredit() || !accounts.creditAccount().isArchived()) &&
                    (!accounts.hasDebit() || !accounts.debitAccount().isArchived())
                ),
                "Linked accounts must not be archived."
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
