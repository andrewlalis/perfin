package com.andrewlalis.perfin.view;

import com.andrewlalis.perfin.model.Account;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public class AccountComboBoxCellFactory implements Callback<ListView<Account>, ListCell<Account>> {
    public static class AccountListCell extends ListCell<Account> {
        private final Label label = new Label();

        public AccountListCell() {
            label.setStyle("-fx-text-fill: black;");
        }

        @Override
        protected void updateItem(Account item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                label.setText("None");
            } else {
                label.setText(item.getName() + " (" + item.getAccountNumberSuffix() + ")");
            }
            setGraphic(label);
        }
    }

    @Override
    public ListCell<Account> call(ListView<Account> param) {
        return new AccountListCell();
    }
}
