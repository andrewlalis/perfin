package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.SceneUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import java.util.function.Consumer;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountsViewController implements RouteSelectionListener {
    @FXML
    public BorderPane mainContainer;
    @FXML
    public FlowPane accountsPane;
    @FXML
    public Label noAccountsLabel;

    private final ObservableList<Account> accountsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Sync the size of the accounts pane to its container.
        accountsPane.minWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWrapLengthProperty().bind(mainContainer.widthProperty());
        accountsPane.maxWidthProperty().bind(mainContainer.widthProperty());

        // Map each account in our list to an account tile element.
        BindingUtil.mapContent(accountsPane.getChildren(), accountsList, account -> SceneUtil.loadNode(
                "/account-tile.fxml",
                (Consumer<AccountTileController>) c -> c.setAccount(account)
        ));

        // Show the "no accounts" label when the accountsList is empty.
        var listProp = new SimpleListProperty<>(accountsList);
        noAccountsLabel.visibleProperty().bind(listProp.emptyProperty());
        noAccountsLabel.managedProperty().bind(noAccountsLabel.visibleProperty());
        accountsPane.visibleProperty().bind(listProp.emptyProperty().not());
        accountsPane.managedProperty().bind(accountsPane.visibleProperty());
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
            accountsList.setAll(profile.getDataSource().getAccountRepository().findAll());
        });
    }
}
