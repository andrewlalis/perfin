package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.view.AccountComboBoxCellFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;

import java.util.List;

/**
 * A box that allows the user to select one account from a list of options.
 */
public class AccountSelectionBox extends ComboBox<Account> {
    private final BooleanProperty allowNoneProperty = new SimpleBooleanProperty(true);

    public AccountSelectionBox() {
        valueProperty().bind(getSelectionModel().selectedItemProperty());
        var factory = new AccountComboBoxCellFactory();
        setCellFactory(factory);
        setButtonCell(factory.call(null));
    }

    public void setAccounts(List<Account> accounts) {
        if (getAllowNone() && !accounts.contains(null)) {
            accounts.add(null);
        }
        getItems().clear();
        getItems().addAll(accounts);
        int idx;
        if (getAllowNone()) {
            getSelectionModel().select(null);
            idx = accounts.indexOf(null);
        } else {
            getSelectionModel().clearSelection();
            idx = 0;
        }
        getButtonCell().updateIndex(idx);
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
}
