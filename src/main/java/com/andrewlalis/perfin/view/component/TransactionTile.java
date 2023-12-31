package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.CreditAndDebitAccounts;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Transaction;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.CompletableFuture;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A tile that displays a transaction's basic information.
 */
public class TransactionTile extends BorderPane {
    public final BooleanProperty selected = new SimpleBooleanProperty(false);
    private static final String UNSELECTED_STYLE = """
            -fx-border-color: lightgray;
            -fx-border-width: 1px;
            -fx-border-style: solid;
            -fx-border-radius: 5px;
            -fx-padding: 5px;
            -fx-cursor: hand;
            """;
    private static final String SELECTED_STYLE = """
            -fx-border-color: white;
            -fx-border-width: 1px;
            -fx-border-style: solid;
            -fx-border-radius: 5px;
            -fx-padding: 5px;
            -fx-cursor: hand;
            """;

    public TransactionTile(Transaction transaction) {
        setStyle(UNSELECTED_STYLE);

        setTop(getHeader(transaction));
        setCenter(getBody(transaction));
        setBottom(getFooter(transaction));

        styleProperty().bind(selected.map(value -> value ? SELECTED_STYLE : UNSELECTED_STYLE));
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
            accounts.ifCredit(acc -> {
                Hyperlink link = new Hyperlink(acc.getShortName());
                link.setOnAction(event -> router.navigate("account", acc));
                Text prefix = new Text("Credited from");
                prefix.setFill(Color.RED);
                Platform.runLater(() -> bodyVBox.getChildren().add(new TextFlow(prefix, link)));
            });
            accounts.ifDebit(acc -> {
                Hyperlink link = new Hyperlink(acc.getShortName());
                link.setOnAction(event -> router.navigate("account", acc));
                Text prefix = new Text("Debited to");
                prefix.setFill(Color.GREEN);
                Platform.runLater(() -> bodyVBox.getChildren().add(new TextFlow(prefix, link)));
            });
        });
        return bodyVBox;
    }

    private Node getFooter(Transaction transaction) {
        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(transaction.getTimestamp()));
        HBox footerHBox = new HBox(
                timestampLabel
        );
        footerHBox.setStyle("""
                -fx-spacing: 3px;
                -fx-font-size: small;
                """);
        return footerHBox;
    }

    private CompletableFuture<CreditAndDebitAccounts> getCreditAndDebitAccounts(Transaction transaction) {
        CompletableFuture<CreditAndDebitAccounts> cf = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useTransactionRepository(repo -> {
                CreditAndDebitAccounts accounts = repo.findLinkedAccounts(transaction.getId());
                cf.complete(accounts);
            });
        });
        return cf;
    }
}
