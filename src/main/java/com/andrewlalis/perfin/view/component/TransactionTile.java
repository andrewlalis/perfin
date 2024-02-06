package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.TransactionRepository;
import com.andrewlalis.perfin.data.TransactionVendorRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
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
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A tile that displays a transaction's basic information.
 */
public class TransactionTile extends BorderPane {
    public final BooleanProperty selected = new SimpleBooleanProperty(false);

    public TransactionTile(Transaction transaction) {
        getStyleClass().addAll("tile", "hand-cursor");

        setTop(getHeader(transaction));
        setCenter(getBody(transaction));
        setBottom(getFooter(transaction));
        setRight(getExtra(transaction));

        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                getStyleClass().add("tile-selected");
            } else {
                getStyleClass().remove("tile-selected");
            }
        });
    }

    private Node getHeader(Transaction transaction) {
        Label headerLabel = new Label("Transaction #" + transaction.id);
        headerLabel.getStyleClass().addAll("bold-text");
        return headerLabel;
    }

    private Node getBody(Transaction transaction) {
        PropertiesPane propertiesPane = new PropertiesPane(150);
        Label amountLabel = new Label("Amount");
        amountLabel.getStyleClass().add("bold-text");
        Label amountValue = new Label(CurrencyUtil.formatMoneyWithCurrencyPrefix(transaction.getMoneyAmount()));
        amountValue.getStyleClass().add("mono-font");

        Label descriptionLabel = new Label("Description");
        descriptionLabel.getStyleClass().add("bold-text");
        Label descriptionValue = new Label(transaction.getDescription());
        descriptionValue.setWrapText(true);

        propertiesPane.getChildren().addAll(
                amountLabel, amountValue,
                descriptionLabel, descriptionValue
        );

        VBox bodyVBox = new VBox(
                propertiesPane
        );
        Profile.getCurrent().dataSource().mapRepoAsync(
                TransactionRepository.class,
                repo -> repo.findLinkedAccounts(transaction.id)
        ).thenAccept(accounts -> {
            accounts.ifCredit(acc -> {
                Hyperlink link = new Hyperlink(acc.getShortName());
                link.setOnAction(event -> router.navigate("account", acc));
                Text prefix = new Text("Credited from");
                prefix.getStyleClass().add("negative-color-fill");
                Platform.runLater(() -> bodyVBox.getChildren().add(new TextFlow(prefix, link)));
            });
            accounts.ifDebit(acc -> {
                Hyperlink link = new Hyperlink(acc.getShortName());
                link.setOnAction(event -> router.navigate("account", acc));
                Text prefix = new Text("Debited to");
                prefix.getStyleClass().add("positive-color-fill");
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
        footerHBox.getStyleClass().addAll("std-spacing", "small-font");
        return footerHBox;
    }

    private Node getExtra(Transaction transaction) {
        VBox content = new VBox();
        if (transaction.getCategoryId() != null) {
            Profile.getCurrent().dataSource().mapRepoAsync(
                    TransactionCategoryRepository.class,
                    repo -> repo.findById(transaction.getCategoryId()).orElse(null)
            ).thenAccept(category -> {
                if (category == null) return;
                Platform.runLater(() -> content.getChildren().add(new CategoryLabel(category)));
            });
        }
        if (transaction.getVendorId() != null) {
            Profile.getCurrent().dataSource().mapRepoAsync(
                    TransactionVendorRepository.class,
                    repo -> repo.findById(transaction.getVendorId()).orElse(null)
            ).thenAccept(vendor -> {
                if (vendor == null) return;
                Platform.runLater(() -> content.getChildren().addLast(new Text("@ " + vendor.getName())));
            });
        }
        return content;
    }
}
