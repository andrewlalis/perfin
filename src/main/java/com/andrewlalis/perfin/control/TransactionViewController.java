package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.AttachmentPreview;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class TransactionViewController {
    private Transaction transaction;

    @FXML public Label titleLabel;

    @FXML public Label amountLabel;
    @FXML public Label timestampLabel;
    @FXML public Label descriptionLabel;

    @FXML public Hyperlink debitAccountLink;
    @FXML public Hyperlink creditAccountLink;

    @FXML public VBox attachmentsContainer;
    @FXML public HBox attachmentsHBox;
    private final ObservableList<Attachment> attachmentsList = FXCollections.observableArrayList();

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
        if (transaction == null) return;
        titleLabel.setText("Transaction #" + transaction.id);
        amountLabel.setText(CurrencyUtil.formatMoney(transaction.getMoneyAmount()));
        timestampLabel.setText(DateUtil.formatUTCAsLocalWithZone(transaction.getTimestamp()));
        descriptionLabel.setText(transaction.getDescription());

        configureAccountLinkBindings(debitAccountLink);
        configureAccountLinkBindings(creditAccountLink);
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                CreditAndDebitAccounts accounts = repo.findLinkedAccounts(transaction.id);
                Platform.runLater(() -> {
                    accounts.ifDebit(acc -> {
                        debitAccountLink.setText(acc.getShortName());
                        debitAccountLink.setOnAction(event -> router.navigate("account", acc));
                    });
                    accounts.ifCredit(acc -> {
                        creditAccountLink.setText(acc.getShortName());
                        creditAccountLink.setOnAction(event -> router.navigate("account", acc));
                    });
                });
            });
        });

        attachmentsContainer.managedProperty().bind(attachmentsContainer.visibleProperty());
        attachmentsContainer.visibleProperty().bind(new SimpleListProperty<>(attachmentsList).emptyProperty().not());
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                List<Attachment> attachments = repo.findAttachments(transaction.id);
                Platform.runLater(() -> attachmentsList.setAll(attachments));
            });
        });
        attachmentsHBox.setMinHeight(AttachmentPreview.HEIGHT);
        attachmentsHBox.setPrefHeight(AttachmentPreview.HEIGHT);
        ((ScrollPane) attachmentsHBox.getParent().getParent().getParent()).minHeightProperty().bind(attachmentsHBox.heightProperty().map(n -> n.doubleValue() + 2));
        ((ScrollPane) attachmentsHBox.getParent().getParent().getParent()).prefHeightProperty().bind(attachmentsHBox.heightProperty().map(n -> n.doubleValue() + 2));
        BindingUtil.mapContent(attachmentsHBox.getChildren(), attachmentsList, AttachmentPreview::new);
    }

    @FXML public void deleteTransaction() {
        boolean confirm = Popups.confirm(
            "Are you sure you want to delete this transaction? This will " +
            "permanently remove the transaction and its effects on any linked " +
            "accounts, as well as remove any attachments from storage within " +
            "this app.\n\n" +
            "Note that incorrect or missing transactions can cause your " +
            "account's balance to be incorrectly reported in Perfin, because " +
            "it's derived from the most recent balance-record, and transactions."
        );
        if (confirm) {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                // TODO: Delete attachments first!
                repo.delete(transaction.id);
                router.replace("transactions");
            });
        }
    }

    private void configureAccountLinkBindings(Hyperlink link) {
        TextFlow parent = (TextFlow) link.getParent();
        parent.managedProperty().bind(parent.visibleProperty());
        parent.visibleProperty().bind(link.textProperty().isNotEmpty());
        link.setText(null);
    }
}
