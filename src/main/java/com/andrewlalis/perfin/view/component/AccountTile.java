package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.Profile;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.Map;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A compact tile that displays information about an account.
 */
public class AccountTile extends BorderPane {
    private static final Map<AccountType, Color> ACCOUNT_TYPE_COLORS = Map.of(
            AccountType.CHECKING, Color.rgb(3, 127, 252),
            AccountType.SAVINGS, Color.rgb(57, 158, 74),
            AccountType.CREDIT_CARD, Color.rgb(207, 8, 68)
    );

    public AccountTile(Account account) {
        setPrefWidth(350.0);
        setStyle("""
                -fx-border-color: lightgray;
                -fx-border-width: 1px;
                -fx-border-style: solid;
                -fx-border-radius: 5px;
                -fx-padding: 5px;
                -fx-cursor: hand;
                """);

        setTop(getHeader(account));
        setBottom(getFooter(account));
        setCenter(getBody(account));

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> router.navigate("account", account));
    }

    private Node getHeader(Account account) {
        Text title = new Text("Account #" + account.id);
        title.setStyle("-fx-font-size: large; -fx-font-weight: bold;");
        return title;
    }

    private Node getFooter(Account account) {
        Label currencyLabel = new Label(account.getCurrency().getCurrencyCode());
        Label typeLabel = new Label(account.getType().toString() + " Account");
        HBox footerHBox = new HBox(currencyLabel, typeLabel);
        footerHBox.setStyle("-fx-font-size: x-small; -fx-spacing: 3px;");
        return footerHBox;
    }

    private Node getBody(Account account) {
        PropertiesPane propertiesPane = new PropertiesPane();
        propertiesPane.setHgap(3);
        propertiesPane.setVgap(3);
        ColumnConstraints keyConstraints = new ColumnConstraints();
        keyConstraints.setMinWidth(150);
        keyConstraints.setHgrow(Priority.NEVER);
        keyConstraints.setHalignment(HPos.LEFT);
        ColumnConstraints valueConstraints = new ColumnConstraints();
        valueConstraints.setHgrow(Priority.ALWAYS);
        valueConstraints.setHalignment(HPos.RIGHT);
        propertiesPane.getColumnConstraints().setAll(keyConstraints, valueConstraints);

        Label accountNameLabel = newPropertyValue(account.getName());
        accountNameLabel.setWrapText(true);

        Label accountTypeLabel = newPropertyValue(account.getType().toString());
        accountTypeLabel.setTextFill(ACCOUNT_TYPE_COLORS.get(account.getType()));
        accountTypeLabel.setStyle("-fx-font-weight: bold;");

        Label balanceLabel = newPropertyValue("Computing balance...");
        balanceLabel.setDisable(true);
        Profile.getCurrent().getDataSource().getAccountBalanceText(account, text -> {
            balanceLabel.setText(text);
            balanceLabel.setDisable(false);
        });

        propertiesPane.getChildren().addAll(
                newPropertyLabel("Account Name"),
                accountNameLabel,
                newPropertyLabel("Account Number"),
                newPropertyValue(account.getAccountNumber()),
                newPropertyLabel("Account Type"),
                accountTypeLabel,
                newPropertyLabel("Current Balance"),
                balanceLabel
        );
        return propertiesPane;
    }

    private static Label newPropertyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("""
                -fx-font-weight: bold;
                """);
        return lbl;
    }

    private static Label newPropertyValue(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("""
                -fx-font-family: monospace;
                -fx-font-size: large;
                """);
        return lbl;
    }
}
