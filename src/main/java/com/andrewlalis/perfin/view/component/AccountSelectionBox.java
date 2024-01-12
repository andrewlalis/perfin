package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.Account;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.List;

/**
 * A box that allows the user to select one account from a list of options.
 */
public class AccountSelectionBox extends ComboBox<Account> {
    private final CellFactory cellFactory = new CellFactory();
    private final BooleanProperty allowNoneProperty = new SimpleBooleanProperty(true);

    public AccountSelectionBox() {
        setCellFactory(cellFactory);
        setButtonCell(cellFactory.call(null));
    }

    public void setAccounts(List<Account> accounts) {
        if (getAllowNone() && !accounts.contains(null)) {
            accounts.add(null);
        }
        getItems().clear();
        getItems().addAll(accounts);
        if (getAllowNone()) {
            getSelectionModel().select(null);
        } else {
            getSelectionModel().clearSelection();
        }
    }

    public void select(Account account) {
        setButtonCell(cellFactory.call(null));
        getSelectionModel().select(account);
    }

    public final BooleanProperty allowNoneProperty() {
        return allowNoneProperty;
    }

    public final boolean getAllowNone() {
        return allowNoneProperty.get();
    }

    public final void setAllowNone(boolean value) {
        allowNoneProperty.set(value);
    }

    private static class CellFactory implements Callback<ListView<Account>, ListCell<Account>> {
        @Override
        public ListCell<Account> call(ListView<Account> param) {
            return new AccountListCell();
        }
    }

    private static class AccountListCell extends ListCell<Account> {
        private final Label label = new Label();

        public AccountListCell() {
            setGraphic(label);
            label.getStyleClass().add("normal-color-text-fill");
        }

        @Override
        protected void updateItem(Account item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                label.setText("None");
            } else {
                label.setText(item.getName() + " " + item.getAccountNumberSuffix());
            }
        }
    }
}
