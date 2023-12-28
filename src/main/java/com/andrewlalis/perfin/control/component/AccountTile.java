package com.andrewlalis.perfin.control.component;

import com.andrewlalis.perfin.data.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.Profile;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A compact tile that displays information about an account.
 */
public class AccountTile extends BorderPane {
    public final Label accountNumberLabel = newPropertyValue();
    public final Label accountBalanceLabel = newPropertyValue();
    public final VBox accountNameBox = new VBox();
    public final Label accountNameLabel = newPropertyValue();

    private static final Map<AccountType, Color> ACCOUNT_TYPE_COLORS = Map.of(
            AccountType.CHECKING, Color.rgb(214, 222, 255),
            AccountType.SAVINGS, Color.rgb(219, 255, 214),
            AccountType.CREDIT_CARD, Color.rgb(255, 250, 214)
    );

    public AccountTile(Account account) {
        setPrefWidth(300.0);
        setPrefHeight(100.0);
        setStyle("""
                -fx-border-color: lightgray;
                -fx-border-width: 1px;
                -fx-border-style: solid;
                -fx-border-radius: 5px;
                -fx-padding: 5px;
                -fx-cursor: hand;
                """);
        Color color = ACCOUNT_TYPE_COLORS.get(account.getType());
        var fill = new BackgroundFill(color, new CornerRadii(3.0), null);
        setBackground(new Background(fill));

        accountNameBox.getChildren().setAll(
                newPropertyLabel("Account Name"),
                accountNameLabel
        );

        Label currencyLabel = new Label(account.getCurrency().getCurrencyCode());
        Label typeLabel = new Label(account.getType().toString() + " Account");
        HBox footerHBox = new HBox(currencyLabel, typeLabel);
        footerHBox.setStyle("-fx-font-size: x-small; -fx-spacing: 3px;");
        setBottom(footerHBox);

        setCenter(new VBox(
                newPropertyLabel("Account Number"),
                accountNumberLabel,
                newPropertyLabel("Account Balance"),
                accountBalanceLabel,
                accountNameBox
        ));

        ObservableValue<Boolean> accountNameTextPresent = accountNameLabel.textProperty().map(t -> t != null && !t.isBlank());
        accountNameBox.visibleProperty().bind(accountNameTextPresent);
        accountNameBox.managedProperty().bind(accountNameTextPresent);

        accountNumberLabel.setText(account.getAccountNumber());
        accountNameLabel.setText(account.getName());
        accountBalanceLabel.setText("Loading balance...");
        accountBalanceLabel.setDisable(true);
        Profile.getCurrent().getDataSource().getAccountBalanceText(account, balanceText -> {
            accountBalanceLabel.setText(balanceText);
            accountBalanceLabel.setDisable(false);
        });

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            router.navigate("account", account);
        });
    }

    private static Label newPropertyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("""
                -fx-font-weight: bold;
                """);
        return lbl;
    }

    private static Label newPropertyValue() {
        Label lbl = new Label();
        lbl.setStyle("""
                -fx-font-family: monospace;
                -fx-font-size: large;
                """);
        return lbl;
    }
}
