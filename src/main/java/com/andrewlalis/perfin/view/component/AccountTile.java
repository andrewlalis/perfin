package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.util.Map;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * A compact tile that displays information about an account.
 */
public class AccountTile extends BorderPane {
    private static final Map<AccountType, String> ACCOUNT_TYPE_COLORS = Map.of(
            AccountType.CHECKING, "-fx-theme-account-type-checking",
            AccountType.SAVINGS, "-fx-theme-account-type-savings",
            AccountType.CREDIT_CARD, "-fx-theme-account-type-credit-card"
    );

    public AccountTile(Account account) {
        setPrefWidth(350.0);
        getStyleClass().addAll("tile", "hand-cursor");

        setTop(getHeader(account));
        setBottom(getFooter(account));
        setCenter(getBody(account));

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> router.navigate("account", account));
    }

    private Node getHeader(Account account) {
        Label titleLabel = new Label(account.getName());
        titleLabel.getStyleClass().addAll("large-font", "bold-text");
        return titleLabel;
    }

    private Node getFooter(Account account) {
        Label currencyLabel = new Label(account.getCurrency().getCurrencyCode());
        Label typeLabel = new Label(account.getType().toString() + " Account");
        HBox footerHBox = new HBox(currencyLabel, typeLabel);
        footerHBox.getStyleClass().addAll("std-spacing", "small-font");
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

        Label accountNumberLabel = new Label(account.getAccountNumber());
        accountNumberLabel.getStyleClass().add("mono-font");

        Label accountTypeLabel = new Label(account.getType().toString());
        accountTypeLabel.getStyleClass().add("bold-text");
        accountTypeLabel.setStyle("-fx-text-fill: " + ACCOUNT_TYPE_COLORS.get(account.getType()));

        Label balanceLabel = new Label("Computing balance...");
        balanceLabel.getStyleClass().addAll("mono-font");
        balanceLabel.setDisable(true);
        Thread.ofVirtual().start(() -> Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
            BigDecimal balance = repo.deriveCurrentBalance(account.id);
            String text = CurrencyUtil.formatMoney(new MoneyValue(balance, account.getCurrency()));
            Platform.runLater(() -> {
                balanceLabel.setText(text);
                if (account.getType().areDebitsPositive() && balance.compareTo(BigDecimal.ZERO) < 0) {
                    balanceLabel.getStyleClass().add("negative-color-text-fill");
                } else if (!account.getType().areDebitsPositive() && balance.compareTo(BigDecimal.ZERO) < 0) {
                    balanceLabel.getStyleClass().add("positive-color-text-fill");
                }
                balanceLabel.setDisable(false);
            });
        }));
        Profile.getCurrent().getDataSource().getAccountBalanceText(account, text -> {
            balanceLabel.setText(text);
            balanceLabel.setDisable(false);
        });

        propertiesPane.getChildren().addAll(
                newPropertyLabel("Account Number"),
                accountNumberLabel,
                newPropertyLabel("Account Type"),
                accountTypeLabel,
                newPropertyLabel("Current Balance"),
                balanceLabel
        );
        return propertiesPane;
    }

    private static Label newPropertyLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("bold-text");
        return lbl;
    }
}
