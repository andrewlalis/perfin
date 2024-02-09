package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.*;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.AttachmentsViewPane;
import com.andrewlalis.perfin.view.component.PropertiesPane;
import com.andrewlalis.perfin.view.component.TransactionLineItemTile;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.andrewlalis.perfin.PerfinApp.router;

public class TransactionViewController {
    private static final Logger log = LoggerFactory.getLogger(TransactionViewController.class);

    private final ObjectProperty<Transaction> transactionProperty = new SimpleObjectProperty<>(null);
    private final ObservableValue<Currency> observableCurrency = transactionProperty.map(Transaction::getCurrency);
    private final ObjectProperty<CreditAndDebitAccounts> linkedAccountsProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TransactionVendor> vendorProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TransactionCategory> categoryProperty = new SimpleObjectProperty<>(null);
    private final ObservableList<String> tagsList = FXCollections.observableArrayList();
    private final ListProperty<String> tagsListProperty = new SimpleListProperty<>(tagsList);
    private final ObservableList<TransactionLineItem> lineItemsList = FXCollections.observableArrayList();
    private final ListProperty<TransactionLineItem> lineItemsProperty = new SimpleListProperty<>(lineItemsList);
    private final ObservableList<Attachment> attachmentsList = FXCollections.observableArrayList();

    @FXML public Label titleLabel;

    @FXML public Label amountLabel;
    @FXML public Label timestampLabel;
    @FXML public Label descriptionLabel;

    @FXML public Label vendorLabel;
    @FXML public Circle categoryColorIndicator;
    @FXML public Label categoryLabel;
    @FXML public Label tagsLabel;

    @FXML public Hyperlink debitAccountLink;
    @FXML public Hyperlink creditAccountLink;

    @FXML public VBox lineItemsVBox;

    @FXML public AttachmentsViewPane attachmentsViewPane;

    @FXML public void initialize() {
        titleLabel.textProperty().bind(transactionProperty.map(t -> "Transaction #" + t.id));
        amountLabel.textProperty().bind(transactionProperty.map(t -> CurrencyUtil.formatMoney(t.getMoneyAmount())));
        timestampLabel.textProperty().bind(transactionProperty.map(t -> DateUtil.formatUTCAsLocalWithZone(t.getTimestamp())));
        descriptionLabel.textProperty().bind(transactionProperty.map(Transaction::getDescription));

        PropertiesPane vendorPane = (PropertiesPane) vendorLabel.getParent();
        BindingUtil.bindManagedAndVisible(vendorPane, vendorProperty.isNotNull());
        vendorLabel.textProperty().bind(vendorProperty.map(TransactionVendor::getName));

        PropertiesPane categoryPane = (PropertiesPane) categoryLabel.getParent().getParent();
        BindingUtil.bindManagedAndVisible(categoryPane, categoryProperty.isNotNull());
        categoryLabel.textProperty().bind(categoryProperty.map(TransactionCategory::getName));
        categoryColorIndicator.fillProperty().bind(categoryProperty.map(TransactionCategory::getColor));

        PropertiesPane tagsPane = (PropertiesPane) tagsLabel.getParent();
        BindingUtil.bindManagedAndVisible(tagsPane, tagsListProperty.emptyProperty().not());
        tagsLabel.textProperty().bind(tagsListProperty.map(tags -> String.join(", ", tags)));

        TextFlow debitText = (TextFlow) debitAccountLink.getParent();
        BindingUtil.bindManagedAndVisible(debitText, linkedAccountsProperty.map(CreditAndDebitAccounts::hasDebit));
        debitAccountLink.textProperty().bind(linkedAccountsProperty.map(la -> la.hasDebit() ? la.debitAccount().getShortName() : null));
        debitAccountLink.onActionProperty().bind(linkedAccountsProperty.map(la -> {
            if (la.hasDebit()) {
                return event -> router.navigate("account", la.debitAccount());
            }
            return event -> {};
        }));
        TextFlow creditText = (TextFlow) creditAccountLink.getParent();
        BindingUtil.bindManagedAndVisible(creditText, linkedAccountsProperty.map(CreditAndDebitAccounts::hasCredit));
        creditAccountLink.textProperty().bind(linkedAccountsProperty.map(la -> la.hasCredit() ? la.creditAccount().getShortName() : null));
        creditAccountLink.onActionProperty().bind(linkedAccountsProperty.map(la -> {
            if (la.hasCredit()) {
                return event -> router.navigate("account", la.creditAccount());
            }
            return event -> {};
        }));

        VBox lineItemsContainer = (VBox) lineItemsVBox.getParent();
        BindingUtil.bindManagedAndVisible(lineItemsContainer, lineItemsProperty.emptyProperty().not());
        lineItemsProperty.addListener((observable, oldValue, newValue) -> {
            lineItemsVBox.getChildren().clear();
            Label loadingLabel = new Label("Loading line items...");
            loadingLabel.getStyleClass().addAll("secondary-color-text-fill");
            lineItemsVBox.getChildren().add(loadingLabel);
            List<CompletableFuture<TransactionLineItemTile>> tileFutures = lineItemsList.stream()
                    .map(item -> TransactionLineItemTile.build(item, observableCurrency, null))
                    .toList();
            Thread.ofVirtual().start(() -> {
                List<TransactionLineItemTile> tiles = tileFutures.stream()
                        .map(CompletableFuture::join).toList();
                Platform.runLater(() -> {
                    lineItemsVBox.getChildren().remove(loadingLabel);
                    lineItemsVBox.getChildren().addAll(tiles);
                });
            });
        });

        attachmentsViewPane.hideIfEmpty();
        attachmentsViewPane.listProperty().bindContent(attachmentsList);

        transactionProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                linkedAccountsProperty.set(null);
                vendorProperty.set(null);
                categoryProperty.set(null);
                tagsList.clear();
                lineItemsList.clear();
                attachmentsList.clear();
            } else {
                updateLinkedData(newValue);
            }
        });
    }

    public void setTransaction(Transaction transaction) {
        this.transactionProperty.set(transaction);
    }

    private void updateLinkedData(Transaction tx) {
        var ds = Profile.getCurrent().dataSource();
        Thread.ofVirtual().start(() -> {
            try (
                var transactionRepo = ds.getTransactionRepository();
                var vendorRepo = ds.getTransactionVendorRepository();
                var categoryRepo = ds.getTransactionCategoryRepository();
                var lineItemsRepo = ds.getTransactionLineItemRepository()
            ) {
                final var linkedAccounts = transactionRepo.findLinkedAccounts(tx.id);
                final var vendor = tx.getVendorId() == null ? null : vendorRepo.findById(tx.getVendorId()).orElse(null);
                final var category = tx.getCategoryId() == null ? null : categoryRepo.findById(tx.getCategoryId()).orElse(null);
                final var attachments = transactionRepo.findAttachments(tx.id);
                final var tags = transactionRepo.findTags(tx.id);
                final var lineItems = lineItemsRepo.findItems(tx.id);
                Platform.runLater(() -> {
                    linkedAccountsProperty.set(linkedAccounts);
                    vendorProperty.set(vendor);
                    categoryProperty.set(category);
                    attachmentsList.setAll(attachments);
                    tagsList.setAll(tags);
                    lineItemsList.setAll(lineItems);
                });
            } catch (Exception e) {
                log.error("Failed to fetch additional transaction data.", e);
                Popups.errorLater(titleLabel, e);
            }
        });
    }

    @FXML public void editTransaction() {
        router.navigate("edit-transaction", this.transactionProperty.get());
    }

    @FXML public void deleteTransaction() {
        boolean confirm = Popups.confirm(
            titleLabel,
            "Are you sure you want to delete this transaction? This will " +
            "permanently remove the transaction and its effects on any linked " +
            "accounts, as well as remove any attachments from storage within " +
            "this app.\n\n" +
            "Note that incorrect or missing transactions can cause your " +
            "account's balance to be incorrectly reported in Perfin, because " +
            "it's derived from the most recent balance-record, and transactions."
        );
        if (confirm) {
            Profile.getCurrent().dataSource().useRepo(TransactionRepository.class, repo -> repo.delete(this.transactionProperty.get().id));
            router.replace("transactions");
        }
    }
}
