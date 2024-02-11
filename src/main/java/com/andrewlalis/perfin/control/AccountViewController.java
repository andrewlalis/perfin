package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.BindingUtil;
import com.andrewlalis.perfin.view.component.AccountHistoryView;
import com.andrewlalis.perfin.view.component.PropertiesPane;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.validators.PredicateValidator;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.time.*;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountViewController implements RouteSelectionListener {
    private final ObjectProperty<Account> accountProperty = new SimpleObjectProperty<>(null);
    private final ObservableValue<Boolean> accountArchived = accountProperty.map(a -> a != null && a.isArchived());
    private final StringProperty balanceTextProperty = new SimpleStringProperty(null);

    @FXML public Label titleLabel;
    @FXML public Label accountNameLabel;
    @FXML public Label accountNumberLabel;
    @FXML public Label accountCurrencyLabel;
    @FXML public Label accountCreatedAtLabel;
    @FXML public Label accountBalanceLabel;
    @FXML public PropertiesPane descriptionPane;
    @FXML public Text accountDescriptionText;

    @FXML public AccountHistoryView accountHistory;

    @FXML public HBox actionsBox;

    @FXML public DatePicker balanceCheckerDatePicker;
    @FXML public Button balanceCheckerButton;

    @FXML public void initialize() {
        titleLabel.textProperty().bind(accountProperty.map(a -> "Account #" + a.id));
        accountNameLabel.textProperty().bind(accountProperty.map(Account::getName));
        accountNumberLabel.textProperty().bind(accountProperty.map(Account::getAccountNumber));
        accountCurrencyLabel.textProperty().bind(accountProperty.map(a -> a.getCurrency().getDisplayName()));
        accountCreatedAtLabel.textProperty().bind(accountProperty.map(a -> DateUtil.formatUTCAsLocalWithZone(a.getCreatedAt())));
        accountDescriptionText.textProperty().bind(accountProperty.map(Account::getDescription));
        var hasDescription = accountProperty.map(a -> a.getDescription() != null);
        BindingUtil.bindManagedAndVisible(descriptionPane, hasDescription);
        accountBalanceLabel.textProperty().bind(balanceTextProperty);

        actionsBox.getChildren().forEach(node -> {
            Button button = (Button) node;
            ObservableValue<Boolean> buttonActive = accountArchived;
            if (button.getText().equalsIgnoreCase("Unarchive")) {
                buttonActive = BooleanExpression.booleanExpression(buttonActive).not();
            }
            button.disableProperty().bind(buttonActive);
            button.managedProperty().bind(button.visibleProperty());
            button.visibleProperty().bind(button.disableProperty().not());
        });

        var datePickerValid = new ValidationApplier<>(new PredicateValidator<LocalDate>()
                .addPredicate(date -> date.isBefore(LocalDate.now()), "Date must be in the past.")
        ).attach(balanceCheckerDatePicker, balanceCheckerDatePicker.valueProperty());
        balanceCheckerButton.disableProperty().bind(datePickerValid.not());
        balanceCheckerButton.setOnAction(event -> {
            LocalDate date = balanceCheckerDatePicker.getValue();
            final Instant timestamp = date.atStartOfDay(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toInstant();
            Profile.getCurrent().dataSource().mapRepoAsync(
                    AccountRepository.class,
                    repo -> repo.deriveBalance(getAccount().id, timestamp)
            ).thenAccept(balance -> Platform.runLater(() -> {
                String msg = String.format(
                        "Your balance as of %s is %s, according to Perfin's data.",
                        date,
                        CurrencyUtil.formatMoney(new MoneyValue(balance, getAccount().getCurrency()))
                );
                Popups.message(balanceCheckerButton, msg);
            }));
        });

        accountProperty.addListener((observable, oldValue, newValue) -> {
            accountHistory.clear();
            if (newValue == null) {
                balanceTextProperty.set(null);
            } else {
                accountHistory.setAccountId(newValue.id);
                accountHistory.loadMoreHistory();
                Profile.getCurrent().dataSource().getAccountBalanceText(newValue)
                        .thenAccept(s -> Platform.runLater(() -> balanceTextProperty.set(s)));
            }
        });
    }

    @Override
    public void onRouteSelected(Object context) {
        this.accountProperty.set((Account) context);
    }

    @FXML
    public void goToEditPage() {
        router.navigate("edit-account", getAccount());
    }

    @FXML public void goToCreateBalanceRecord() {
        router.navigate("create-balance-record", getAccount());
    }

    @FXML
    public void archiveAccount() {
        boolean confirmResult = Popups.confirm(
                titleLabel,
                "Are you sure you want to archive this account? It will no " +
                        "longer show up in the app normally, and you won't be " +
                        "able to add new transactions to it. You'll still be " +
                        "able to view the account, and you can un-archive it " +
                        "later if you need to."
        );
        if (confirmResult) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.archive(getAccount().id));
            router.replace("accounts");
        }
    }

    @FXML public void unarchiveAccount() {
        boolean confirm = Popups.confirm(
                titleLabel,
                "Are you sure you want to restore this account from its archived " +
                        "status?"
        );
        if (confirm) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.unarchive(getAccount().id));
            router.replace("accounts");
        }
    }

    @FXML
    public void deleteAccount() {
        boolean confirm = Popups.confirm(
                titleLabel,
                "Are you sure you want to permanently delete this account and " +
                        "all data directly associated with it? This cannot be " +
                        "undone; deleted accounts are not recoverable at all. " +
                        "Consider archiving this account instead if you just " +
                        "want to hide it."
        );
        if (confirm) {
            Profile.getCurrent().dataSource().useRepo(AccountRepository.class, repo -> repo.delete(getAccount()));
            router.replace("accounts");
        }
    }

    private Account getAccount() {
        return accountProperty.get();
    }
}
