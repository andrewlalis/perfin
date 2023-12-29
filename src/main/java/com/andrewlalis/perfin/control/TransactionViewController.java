package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.CurrencyUtil;
import com.andrewlalis.perfin.data.DateUtil;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import com.andrewlalis.perfin.model.TransactionAttachment;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class TransactionViewController implements RouteSelectionListener {
    private Transaction transaction;

    @FXML public Label amountLabel;
    @FXML public Label timestampLabel;
    @FXML public Label descriptionLabel;

    @FXML public Hyperlink debitAccountLink;
    @FXML public Hyperlink creditAccountLink;

    @FXML public VBox attachmentsContainer;
    @FXML public HBox attachmentsHBox;
    private final ObservableList<TransactionAttachment> attachmentsList = FXCollections.observableArrayList();

    @Override
    public void onRouteSelected(Object context) {
        this.transaction = (Transaction) context;
        amountLabel.setText(CurrencyUtil.formatMoney(transaction.getAmount(), transaction.getCurrency()));
        timestampLabel.setText(DateUtil.formatUTCAsLocalWithZone(transaction.getTimestamp()));
        descriptionLabel.setText(transaction.getDescription());

        configureAccountLinkBindings(debitAccountLink);
        configureAccountLinkBindings(creditAccountLink);
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                CreditAndDebitAccounts accounts = repo.findLinkedAccounts(transaction.getId());
                Platform.runLater(() -> {
                    if (accounts.hasDebit()) {
                        debitAccountLink.setText(accounts.debitAccount().getShortName());
                        debitAccountLink.setOnAction(event -> router.navigate("account", accounts.debitAccount()));
                    }
                    if (accounts.hasCredit()) {
                        creditAccountLink.setText(accounts.creditAccount().getShortName());
                        creditAccountLink.setOnAction(event -> router.navigate("account", accounts.creditAccount()));
                    }
                });
            });
        });

        attachmentsContainer.managedProperty().bind(attachmentsContainer.visibleProperty());
        attachmentsContainer.visibleProperty().bind(new SimpleListProperty<>(attachmentsList).emptyProperty().not());
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                List<TransactionAttachment> attachments = repo.findAttachments(transaction.getId());
                Platform.runLater(() -> attachmentsList.setAll(attachments));
            });
        });
        BindingUtil.mapContent(attachmentsHBox.getChildren(), attachmentsList, attachment -> {
            VBox vbox = new VBox(
                    new Label(attachment.getFilename()),
                    new Label(attachment.getContentType())
            );
            return vbox;
        });
    }

    @FXML public void deleteTransaction() {
        boolean confirm = Popups.confirm(
            "Are you sure you want to delete this transaction? This will " +
            "permanently remove the transaction and its effects on any linked " +
            "accounts, as well as remove any attachments from storage within " +
            "this app."
        );
        if (confirm) {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                // TODO: Delete attachments first!
                repo.delete(transaction.getId());
                router.getHistory().clear();
                router.navigate("transactions");
            });
        }
    }

    private void configureAccountLinkBindings(Hyperlink link) {
        TextFlow parent = (TextFlow) link.getParent();
        parent.managedProperty().bind(parent.visibleProperty());
        parent.visibleProperty().bind(link.textProperty().isNotEmpty());
    }
}
