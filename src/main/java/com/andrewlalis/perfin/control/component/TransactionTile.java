package com.andrewlalis.perfin.control.component;

import com.andrewlalis.perfin.data.CurrencyUtil;
import com.andrewlalis.perfin.data.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;

import java.util.concurrent.CompletableFuture;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A tile that displays a transaction's basic information.
 */
public class TransactionTile extends BorderPane {
    public TransactionTile(Transaction transaction, Runnable refresh) {
        setStyle("""
                -fx-border-color: lightgray;
                -fx-border-width: 1px;
                -fx-border-style: solid;
                -fx-border-radius: 5px;
                -fx-padding: 5px;
                -fx-max-width: 500px;
                """);

        setTop(getHeader(transaction));
        setCenter(getBody(transaction));
        setBottom(getFooter(transaction, refresh));
    }

    private Node getHeader(Transaction transaction) {
        Label currencyLabel = new Label(CurrencyUtil.formatMoney(transaction.getAmount(), transaction.getCurrency()));
        currencyLabel.setStyle("-fx-font-family: monospace;");
        HBox headerHBox = new HBox(
                currencyLabel
        );
        headerHBox.setStyle("""
                -fx-spacing: 3px;
                """);
        return headerHBox;
    }

    private Node getBody(Transaction transaction) {
        Label descriptionLabel = new Label(transaction.getDescription());
        descriptionLabel.setWrapText(true);
        VBox bodyVBox = new VBox(
                descriptionLabel
        );
        getCreditAndDebitAccounts(transaction).thenAccept(accounts -> {
            Account creditAccount = accounts.getKey();
            Account debitAccount = accounts.getValue();
            if (creditAccount != null) {
                Hyperlink link = new Hyperlink(creditAccount.getShortName());
                link.setOnAction(event -> router.navigate("account", creditAccount));
                TextFlow text = new TextFlow(new Text("Credited from"), link);
                Platform.runLater(() -> bodyVBox.getChildren().add(text));
            } if (debitAccount != null) {
                Hyperlink link = new Hyperlink(debitAccount.getShortName());
                link.setOnAction(event -> router.navigate("account", debitAccount));
                TextFlow text = new TextFlow(new Text("Debited to"), link);
                Platform.runLater(() -> bodyVBox.getChildren().add(text));
            }
        });
        return bodyVBox;
    }

    private Node getFooter(Transaction transaction, Runnable refresh) {
        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(transaction.getTimestamp()));
        Hyperlink deleteLink = new Hyperlink("Delete this transaction");
        deleteLink.setOnAction(event -> {
            var confirmResult = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this transaction?").showAndWait();
            if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                    repo.delete(transaction.getId());
                });
                refresh.run();
            }
        });
        HBox footerHBox = new HBox(
                timestampLabel,
                deleteLink
        );
        footerHBox.setStyle("""
                -fx-spacing: 3px;
                -fx-font-size: small;
                """);
        return footerHBox;
    }

    private CompletableFuture<Pair<Account, Account>> getCreditAndDebitAccounts(Transaction transaction) {
        CompletableFuture<Pair<Account, Account>> cf = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                var entriesAndAccounts = repo.findEntriesWithAccounts(transaction.getId());
                AccountEntry creditEntry = entriesAndAccounts.keySet().stream()
                        .filter(entry -> entry.getType() == AccountEntry.Type.CREDIT)
                        .findFirst().orElse(null);
                AccountEntry debitEntry = entriesAndAccounts.keySet().stream()
                        .filter(entry -> entry.getType() == AccountEntry.Type.DEBIT)
                        .findFirst().orElse(null);
                cf.complete(new Pair<>(entriesAndAccounts.get(creditEntry), entriesAndAccounts.get(debitEntry)));
            });
        });
        return cf;
    }
}
