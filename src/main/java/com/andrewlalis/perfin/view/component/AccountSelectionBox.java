package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.util.List;

/**
 * A box that allows the user to select one account from a list of options.
 */
public class AccountSelectionBox extends ComboBox<Account> {
    private final BooleanProperty allowNoneProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty showBalanceProperty = new SimpleBooleanProperty(false);

    public AccountSelectionBox() {
        setCellFactory(new CellFactory(showBalanceProperty));
        setButtonCell(new AccountListCell(new SimpleBooleanProperty(false)));
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
        setButtonCell(new AccountListCell(new SimpleBooleanProperty(false)));
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

    public final BooleanProperty showBalanceProperty() {
        return showBalanceProperty;
    }

    public final boolean getShowBalance() {
        return showBalanceProperty.get();
    }

    public final void setShowBalance(boolean value) {
        showBalanceProperty.set(value);
    }

    /**
     * A simple cell factory that just returns instances of {@link AccountListCell}.
     * @param showBalanceProp Whether to show the account's balance.
     */
    private record CellFactory(BooleanProperty showBalanceProp) implements Callback<ListView<Account>, ListCell<Account>> {
        @Override
        public ListCell<Account> call(ListView<Account> param) {
            return new AccountListCell(showBalanceProp);
        }
    }

    /**
     * A list cell implementation which shows an account's name, and optionally,
     * its current derived balance underneath.
     */
    private static class AccountListCell extends ListCell<Account> {
        private final BooleanProperty showBalanceProp;
        private final Label nameLabel = new Label();
        private final Label balanceLabel = new Label();

        public AccountListCell(BooleanProperty showBalanceProp) {
            this.showBalanceProp = showBalanceProp;
            nameLabel.getStyleClass().add("normal-color-text-fill");
            balanceLabel.getStyleClass().addAll("secondary-color-text-fill", "mono-font", "smallest-font", "italic-text");
            balanceLabel.managedProperty().bind(balanceLabel.visibleProperty());
            balanceLabel.setVisible(false);
            setGraphic(new VBox(nameLabel, balanceLabel));
        }

        @Override
        protected void updateItem(Account item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                nameLabel.setText("None");
                balanceLabel.setVisible(false);
                return;
            }

            nameLabel.setText(item.getName() + " (" + item.getAccountNumberSuffix() + ")");
            if (showBalanceProp.get()) {
                Profile.getCurrent().dataSource().useRepoAsync(AccountRepository.class, repo -> {
                    BigDecimal balance = repo.deriveCurrentBalance(item.id);
                    Platform.runLater(() -> {
                        balanceLabel.setText(CurrencyUtil.formatMoney(new MoneyValue(balance, item.getCurrency())));
                        balanceLabel.setVisible(true);
                    });
                });
            }
        }
    }
}
