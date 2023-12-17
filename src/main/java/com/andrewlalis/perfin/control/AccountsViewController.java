package com.andrewlalis.perfin.control;

import com.andrewlalis.perfin.SceneUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import java.util.List;
import java.util.function.Consumer;

public class AccountsViewController {
    @FXML
    public BorderPane mainContainer;
    @FXML
    public FlowPane accountsPane;

    private final ObservableList<Account> accountsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        accountsPane.minWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWidthProperty().bind(mainContainer.widthProperty());
        accountsPane.prefWrapLengthProperty().bind(mainContainer.widthProperty());
        accountsPane.maxWidthProperty().bind(mainContainer.widthProperty());

        BindingUtil.mapContent(accountsPane.getChildren(), accountsList, account -> SceneUtil.loadNode(
                "/account-tile.fxml",
                (Consumer<AccountTileController>) c -> c.setAccount(account)
        ));
        Profile.whenLoaded(profile -> {
            populateAccounts(profile.getDataSource().getAccountRepository().findAll());
        });
    }

    private void populateAccounts(List<Account> accounts) {
        this.accountsList.clear();
        this.accountsList.addAll(accounts);
    }
}
