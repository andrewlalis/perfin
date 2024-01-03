package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.pagination.PageRequest;
import com.andrewlalis.perfin.data.pagination.Sort;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.AccountTile;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountsViewController implements RouteSelectionListener {
    @FXML
    public FlowPane accountsPane;
    @FXML
    public Label noAccountsLabel;
    @FXML
    public Label totalLabel;

    private final BooleanProperty noAccounts = new SimpleBooleanProperty(false);

    @FXML
    public void initialize() {
        // Show the "no accounts" label when the accountsList is empty.
        noAccountsLabel.visibleProperty().bind(noAccounts);
        noAccountsLabel.managedProperty().bind(noAccounts);
        accountsPane.visibleProperty().bind(noAccounts.not());
        accountsPane.managedProperty().bind(noAccounts.not());
    }

    @FXML
    public void createNewAccount() {
        router.navigate("edit-account");
    }

    @Override
    public void onRouteSelected(Object context) {
        refreshAccounts();
    }

    public void refreshAccounts() {
        Profile.whenLoaded(profile -> {
            Thread.ofVirtual().start(() -> {
                profile.getDataSource().useAccountRepository(repo -> {
                    var page = repo.findAll(PageRequest.unpaged(Sort.asc("created_at")));
                    Platform.runLater(() -> {
                        accountsPane.getChildren().setAll(page.items().stream().map(AccountTile::new).toList());
                    });
                });
            });
            // Compute grand totals!
            Thread.ofVirtual().start(() -> {
                var totals = profile.getDataSource().getCombinedAccountBalances();
                StringBuilder sb = new StringBuilder("Totals: ");
                for (var entry : totals.entrySet()) {
                    sb.append(CurrencyUtil.formatMoneyWithCurrencyPrefix(new MoneyValue(entry.getValue(), entry.getKey())));
                }
                Platform.runLater(() -> totalLabel.setText(sb.toString().strip()));
            });
        });

    }
}
