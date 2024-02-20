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
import com.andrewlalis.perfin.view.component.CategorySelectionBox;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import com.andrewlalis.perfin.view.component.TransactionLineItemTile;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;
import com.andrewlalis.perfin.view.component.validation.validators.CurrencyAmountValidator;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

/**
 * Controller for the "edit-transaction" view, which is where the user can
 * create or edit transactions.
 */
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
    @FXML public Hyperlink vendorsHyperlink;
    @FXML public CategorySelectionBox categoryComboBox;
    @FXML public Hyperlink categoriesHyperlink;
    @FXML public ComboBox<String> tagsComboBox;
    @FXML public Hyperlink tagsHyperlink;
    @FXML public Button addTagButton;
    @FXML public VBox tagsVBox;
    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();

    @FXML public Spinner<Integer> lineItemQuantitySpinner;
    @FXML public TextField lineItemValueField;
    @FXML public TextField lineItemDescriptionField;
    @FXML public CategorySelectionBox lineItemCategoryComboBox;
    @FXML public Button addLineItemButton;
    @FXML public VBox addLineItemForm;
    @FXML public Button addLineItemAddButton;
    @FXML public Button addLineItemCancelButton;
    @FXML public VBox lineItemsVBox;
    @FXML public Label lineItemsValueMatchLabel;
    @FXML public Button lineItemsAmountSyncButton;
    @FXML public final BooleanProperty addingLineItemProperty = new SimpleBooleanProperty(false);
    private final ObservableList<TransactionLineItem> lineItems = FXCollections.observableArrayList();
    private static long tmpLineItemId = -1L;

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
        var amountValid = new ValidationApplier<>(new CurrencyAmountValidator(() -> currencyChoiceBox.getValue(), false, false) {
            @Override
            public ValidationResult validate(String input) {
                var r = super.validate(input);
                if (!r.isValid()) return r;
                // Check that this amount is enough to cover the total of any line items.
                BigDecimal lineItemsTotal = lineItems.stream().map(TransactionLineItem::getTotalValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal transactionAmount = new BigDecimal(input);
                if (transactionAmount.compareTo(lineItemsTotal) < 0) {
                    String msg = String.format(
                            "Amount must be at least %s to account for line items.",
                            CurrencyUtil.formatMoney(new MoneyValue(lineItemsTotal, currencyChoiceBox.getValue()))
                    );
                    return ValidationResult.of(msg);
                }
                return ValidationResult.valid();
            }
        }).validatedInitially().attachToTextField(
                amountField,
                currencyChoiceBox.valueProperty(),
                new SimpleListProperty<>(lineItems)
        );
        var descriptionValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s == null || s.length() <= 255, "Description is too long.")
        ).validatedInitially().attach(descriptionField, descriptionField.textProperty());
        var linkedAccountsValid = initializeLinkedAccountsValidationUi();
        initializeTagSelectionUi();
        initializeLineItemsUi();

        vendorsHyperlink.setOnAction(event -> router.navigate("vendors"));
        categoriesHyperlink.setOnAction(event -> router.navigate("categories"));
        tagsHyperlink.setOnAction(event -> router.navigate("tags"));

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
        String category = categoryComboBox.getValue() == null ? null : categoryComboBox.getValue().getName();
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
                    lineItems,
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
                        lineItems,
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
        categoryComboBox.select(null);

        addingLineItemProperty.set(false);

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
                    var categoryRepo = ds.getTransactionCategoryRepository();
                    var lineItemRepo = ds.getTransactionLineItemRepository()
            ) {
                // First fetch all the data.
                List<Currency> currencies = accountRepo.findAllUsedCurrencies().stream()
                        .sorted(Comparator.comparing(Currency::getCurrencyCode))
                        .toList();
                List<Account> accounts = accountRepo.findAll(PageRequest.unpaged(Sort.asc("name"))).items();
                final List<Attachment> attachments;
                final var categoryTreeNodes = categoryRepo.findTree();
                final List<String> availableTags = transactionRepo.findAllTags();
                final List<String> tags;
                final CreditAndDebitAccounts linkedAccounts;
                final String vendorName;
                final TransactionCategory category;
                final List<TransactionLineItem> existingLineItems;
                if (transaction == null) {
                    attachments = Collections.emptyList();
                    tags = Collections.emptyList();
                    linkedAccounts = new CreditAndDebitAccounts(null, null);
                    vendorName = null;
                    category = null;
                    existingLineItems = Collections.emptyList();
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
                        category = categoryRepo.findById(transaction.getCategoryId()).orElse(null);
                    } else {
                        category = null;
                    }
                    existingLineItems = lineItemRepo.findItems(transaction.id);
                }
                final List<TransactionVendor> availableVendors = vendorRepo.findAll();
                // Then make updates to the view.
                Platform.runLater(() -> {
                    currencyChoiceBox.getItems().setAll(currencies);
                    creditAccountSelector.setAccounts(accounts);
                    debitAccountSelector.setAccounts(accounts);
                    vendorComboBox.getItems().setAll(availableVendors.stream().map(TransactionVendor::getName).toList());
                    vendorComboBox.setValue(vendorName);
                    categoryComboBox.loadCategories(categoryTreeNodes);
                    categoryComboBox.select(category);
                    tagsComboBox.getItems().setAll(availableTags);
                    attachmentsSelectionArea.clear();
                    attachmentsSelectionArea.addAttachments(attachments);
                    selectedTags.clear();
                    selectedTags.addAll(tags);
                    if (transaction == null) {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                        creditAccountSelector.select(null);
                        debitAccountSelector.select(null);
                    } else {
                        currencyChoiceBox.getSelectionModel().select(transaction.getCurrency());
                        creditAccountSelector.select(linkedAccounts.creditAccount());
                        debitAccountSelector.select(linkedAccounts.debitAccount());
                    }
                    lineItemCategoryComboBox.loadCategories(categoryTreeNodes);
                    lineItemCategoryComboBox.select(null);
                    lineItems.setAll(existingLineItems);
                    container.setDisable(false);
                });
            } catch (Exception e) {
                log.error("Failed to get repositories.", e);
                Platform.runLater(() -> Popups.error(container, "Failed to fetch account-specific data: " + e.getMessage()));
                router.navigateBackAndClear();
            }
        });
    }

    private BooleanExpression initializeLinkedAccountsValidationUi() {
        Property<CreditAndDebitAccounts> linkedAccountsProperty = new SimpleObjectProperty<>(getSelectedAccounts());
        debitAccountSelector.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        creditAccountSelector.valueProperty().addListener((observable, oldValue, newValue) -> linkedAccountsProperty.setValue(getSelectedAccounts()));
        return new ValidationApplier<>(getLinkedAccountsValidator())
                .validatedInitially()
                .attach(linkedAccountsContainer, linkedAccountsProperty, currencyChoiceBox.valueProperty());
    }

    private void initializeTagSelectionUi() {
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
        BindingUtil.mapContent(tagsVBox.getChildren(), selectedTags, this::createTagListTile);
    }

    private Node createTagListTile(String tag) {
        Label label = new Label(tag);
        label.setMaxWidth(Double.POSITIVE_INFINITY);
        label.getStyleClass().addAll("bold-text");
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(event -> selectedTags.remove(tag));
        BorderPane tile = new BorderPane(label);
        tile.setRight(removeButton);
        tile.getStyleClass().addAll("std-spacing");
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        return tile;
    }

    private void initializeLineItemsUi() {
        addLineItemButton.setOnAction(event -> addingLineItemProperty.set(true));
        addLineItemCancelButton.setOnAction(event -> addingLineItemProperty.set(false));
        addingLineItemProperty.addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // The form has been closed.
                lineItemQuantitySpinner.getValueFactory().setValue(1);
                lineItemValueField.setText(null);
                lineItemDescriptionField.setText(null);
                lineItemCategoryComboBox.setValue(categoryComboBox.getValue());
            }
        });
        BindingUtil.bindManagedAndVisible(addLineItemButton, addingLineItemProperty.not());
        BindingUtil.bindManagedAndVisible(addLineItemForm, addingLineItemProperty);
        BindingUtil.mapContent(lineItemsVBox.getChildren(), lineItems, this::createLineItemTile);
        lineItemQuantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 1, 1));
        var lineItemValueValid = new ValidationApplier<>(new CurrencyAmountValidator(() -> currencyChoiceBox.getValue(), false, false))
                .validatedInitially().attachToTextField(lineItemValueField, currencyChoiceBox.valueProperty());
        var lineItemDescriptionValid = new ValidationApplier<>(new PredicateValidator<String>()
                .addTerminalPredicate(s -> s != null && !s.isBlank(), "A description is required.")
                .addPredicate(s -> s.strip().length() <= TransactionLineItem.DESCRIPTION_MAX_LENGTH, "Description is too long.")
                .addPredicate(
                        s -> lineItems.stream().map(TransactionLineItem::getDescription).noneMatch(d -> d.equalsIgnoreCase(s)),
                        "Description must be unique."
                )
        ).validatedInitially().attachToTextField(lineItemDescriptionField);
        var lineItemFormValid = lineItemValueValid.and(lineItemDescriptionValid);
        addLineItemAddButton.disableProperty().bind(lineItemFormValid.not());
        addLineItemAddButton.setOnAction(event -> {
            int quantity = lineItemQuantitySpinner.getValue();
            BigDecimal valuePerItem = new BigDecimal(lineItemValueField.getText());
            String description = lineItemDescriptionField.getText().strip();
            TransactionCategory category = lineItemCategoryComboBox.getValue();
            Long categoryId = category == null ? null : category.id;
            long tmpId = tmpLineItemId--;
            TransactionLineItem tmpItem = new TransactionLineItem(tmpId, -1L, valuePerItem, quantity, -1, description, categoryId);
            lineItems.add(tmpItem);
            addingLineItemProperty.set(false);
        });

        // Logic for showing an indicator when the line items total exactly matches the entered amount.
        ListProperty<TransactionLineItem> lineItemsProperty = new SimpleListProperty<>(lineItems);
        ObservableValue<BigDecimal> lineItemsTotalValue = lineItemsProperty.map(items -> items.stream()
                .map(TransactionLineItem::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ObjectProperty<BigDecimal> amountFieldValue = new SimpleObjectProperty<>(BigDecimal.ZERO);
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                amountFieldValue.set(BigDecimal.ZERO);
            } else {
                try {
                    BigDecimal amount = new BigDecimal(newValue);
                    amountFieldValue.set(amount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : amount);
                } catch (NumberFormatException e) {
                    amountFieldValue.set(BigDecimal.ZERO);
                }
            }
        });
        BooleanProperty lineItemsTotalMatchesAmount = new SimpleBooleanProperty(false);
        lineItemsTotalValue.addListener((observable, oldValue, newValue) -> {
            lineItemsTotalMatchesAmount.set(newValue.compareTo(amountFieldValue.getValue()) == 0);
        });
        amountFieldValue.addListener((observable, oldValue, newValue) -> {
            lineItemsTotalMatchesAmount.set(newValue.compareTo(lineItemsTotalValue.getValue()) == 0);
        });
        BindingUtil.bindManagedAndVisible(lineItemsValueMatchLabel, lineItemsTotalMatchesAmount.and(lineItemsProperty.emptyProperty().not()));

        // Logic for button that syncs line items total to the amount field.
        BindingUtil.bindManagedAndVisible(lineItemsAmountSyncButton, lineItemsTotalMatchesAmount.not().and(lineItemsProperty.emptyProperty().not()));
        lineItemsAmountSyncButton.setOnAction(event -> amountField.setText(
            CurrencyUtil.formatMoneyAsBasicNumber(new MoneyValue(
                lineItemsTotalValue.getValue(),
                currencyChoiceBox.getValue()
            ))
        ));
    }

    private Node createLineItemTile(TransactionLineItem item) {
        TransactionLineItemTile tile = TransactionLineItemTile.build(item, currencyChoiceBox.valueProperty(), categoryComboBox.getItems()).join();
        Button removeButton = new Button("Remove");
        removeButton.setMaxWidth(Double.POSITIVE_INFINITY);
        removeButton.setOnAction(event -> lineItems.remove(item));
        Button moveUpButton = new Button("Move Up");
        moveUpButton.setMaxWidth(Double.POSITIVE_INFINITY);
        moveUpButton.disableProperty().bind(new SimpleListProperty<>(lineItems).map(items -> items.isEmpty() || items.getFirst().equals(item)));
        moveUpButton.setOnAction(event -> {
            int currentIdx = lineItems.indexOf(item);
            lineItems.remove(currentIdx);
            lineItems.add(currentIdx - 1, item);
        });
        Button moveDownButton = new Button("Move Down");
        moveDownButton.setMaxWidth(Double.POSITIVE_INFINITY);
        moveDownButton.disableProperty().bind(new SimpleListProperty<>(lineItems).map(items -> items.isEmpty() || items.getLast().equals(item)));
        moveDownButton.setOnAction(event -> {
            int currentIdx = lineItems.indexOf(item);
            lineItems.remove(currentIdx);
            lineItems.add(currentIdx + 1, item);
        });
        VBox buttonsBox = new VBox(removeButton, moveUpButton, moveDownButton);
        buttonsBox.getStyleClass().addAll("std-spacing");
        tile.setRight(buttonsBox);
        return tile;
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
