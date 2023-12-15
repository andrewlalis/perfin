package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.SceneUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.AccountType;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.function.Consumer;

public class MainController {
    @FXML
    public BorderPane mainContainer;
    @FXML
    public FlowPane accountsPane;

    @FXML
    public void initialize() {
        accountsPane.minWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWrapLengthProperty().bind(mainContainer.widthProperty());
        accountsPane.maxWidthProperty().bind(mainContainer.widthProperty());
        List<Account> tempAccounts = List.of(
                new Account(AccountType.CHECKING, "1234-4324-4321-4143", BigDecimal.valueOf(3745.01), "Main Checking", Currency.getInstance("USD")),
                new Account(AccountType.CHECKING, "1234-4324-4321-4143", BigDecimal.valueOf(3745.01), "Main Checking", Currency.getInstance("USD")),
                new Account(AccountType.CHECKING, "1234-4324-4321-4143", BigDecimal.valueOf(3745.01), "Main Checking", Currency.getInstance("USD")),
                new Account(AccountType.CHECKING, "1234-4324-4321-4143", BigDecimal.valueOf(3745.01), "Main Checking", Currency.getInstance("USD"))
        );
        populateAccounts(tempAccounts);
    }

    private void populateAccounts(List<Account> accounts) {
        accountsPane.getChildren().clear();
        for (var account : accounts) {
            Parent node = SceneUtil.loadNode(
                    "/account-tile.fxml",
                    (Consumer<AccountTileController>) c -> c.setAccount(account)
            );
            accountsPane.getChildren().add(node);
        }
    }
}
