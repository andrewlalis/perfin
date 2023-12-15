package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.model.Account;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class AccountTileController {
    private Account account;

    @FXML
    public VBox container;
    @FXML
    public Label accountNumberLabel;
    @FXML
    public Label accountBalanceLabel;
    @FXML
    public VBox accountNameBox;
    @FXML
    public Label accountNameLabel;

    @FXML
    public void initialize() {
        ObservableValue<Boolean> accountNameTextPresent = accountNameLabel.textProperty().map(t -> t != null && !t.isBlank());
        accountNameBox.visibleProperty().bind(accountNameTextPresent);
        accountNameBox.managedProperty().bind(accountNameTextPresent);
    }

    public void setAccount(Account account) {
        this.account = account;
        Platform.runLater(() -> {
            accountNumberLabel.setText(account.getAccountNumber());
            accountBalanceLabel.setText(account.getCurrency().getSymbol() + " " + account.getCurrentBalance().toPlainString());
            accountNameLabel.setText(account.getName());
            container.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                System.out.println("Clicked on " + account.getAccountNumber());
            });
        });
    }
}
