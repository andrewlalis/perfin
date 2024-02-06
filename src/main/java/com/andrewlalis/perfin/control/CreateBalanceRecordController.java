package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.BalanceRecordRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import com.andrewlalis.perfin.view.component.PropertiesPane;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
import com.andrewlalis.perfin.view.component.validation.ValidationFunction;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;
import com.andrewlalis.perfin.view.component.validation.validators.CurrencyAmountValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateBalanceRecordController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public TextField balanceField;
    @FXML public Label balanceWarningLabel;
    @FXML public FileSelectionArea attachmentSelectionArea;
    @FXML public PropertiesPane propertiesPane;

    @FXML public Button saveButton;

    private Account account;

    @FXML public void initialize() {
        var timestampValid = new ValidationApplier<>((ValidationFunction<String>)  input -> {
            try {
                DateUtil.DEFAULT_DATETIME_FORMAT.parse(input);
                return ValidationResult.valid();
            } catch (DateTimeParseException e) {
                return ValidationResult.of("Invalid timestamp format.");
            }
        }).validatedInitially().attachToTextField(timestampField);

        var balanceValidator = new CurrencyAmountValidator(() -> account == null ? null : account.getCurrency(), true, false);
        var balanceValid = new ValidationApplier<>(balanceValidator)
                .validatedInitially().attachToTextField(balanceField);

        balanceWarningLabel.managedProperty().bind(balanceWarningLabel.visibleProperty());
        balanceWarningLabel.visibleProperty().set(false);
        balanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!balanceValidator.validate(newValue).isValid() || !timestampValid.get()) {
                balanceWarningLabel.visibleProperty().set(false);
                return;
            }
            BigDecimal reportedBalance = new BigDecimal(newValue);
            LocalDateTime localTimestamp = LocalDateTime.parse(timestampField.getText(), DateUtil.DEFAULT_DATETIME_FORMAT);
            LocalDateTime utcTimestamp = DateUtil.localToUTC(localTimestamp);
            Profile.getCurrent().dataSource().useRepoAsync(AccountRepository.class, repo -> {
                BigDecimal derivedBalance = repo.deriveBalance(account.id, utcTimestamp.toInstant(ZoneOffset.UTC));
                boolean balancesMatch = reportedBalance.setScale(derivedBalance.scale(), RoundingMode.HALF_UP).equals(derivedBalance);
                Platform.runLater(() -> balanceWarningLabel.visibleProperty().set(!balancesMatch));
            });
        });

        var formValid = timestampValid.and(balanceValid);
        saveButton.disableProperty().bind(formValid.not());
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
        Profile.getCurrent().dataSource().useRepoAsync(AccountRepository.class, repo -> {
            BigDecimal value = repo.deriveCurrentBalance(account.id);
            Platform.runLater(() -> balanceField.setText(
                    CurrencyUtil.formatMoneyAsBasicNumber(new MoneyValue(value, account.getCurrency()))
            ));
        });
        attachmentSelectionArea.clear();
    }

    @FXML public void save() {
        LocalDateTime localTimestamp = LocalDateTime.parse(timestampField.getText(), DateUtil.DEFAULT_DATETIME_FORMAT);
        BigDecimal reportedBalance = new BigDecimal(balanceField.getText());

        boolean confirm = Popups.confirm(timestampField, "Are you sure that you want to record the balance of account\n%s\nas %s,\nas of %s?".formatted(
                account.getShortName(),
                CurrencyUtil.formatMoneyWithCurrencyPrefix(new MoneyValue(reportedBalance, account.getCurrency())),
                localTimestamp.atZone(ZoneId.systemDefault()).format(DateUtil.DEFAULT_DATETIME_FORMAT_WITH_ZONE)
        ));
        if (confirm && confirmIfInconsistentBalance(reportedBalance, DateUtil.localToUTC(localTimestamp))) {
            Profile.getCurrent().dataSource().useRepo(BalanceRecordRepository.class, repo -> {
                repo.insert(
                        DateUtil.localToUTC(localTimestamp),
                        account.id,
                        reportedBalance,
                        account.getCurrency(),
                        attachmentSelectionArea.getSelectedPaths()
                );
            });
            router.navigateBackAndClear();
        }
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }

    private boolean confirmIfInconsistentBalance(BigDecimal reportedBalance, LocalDateTime utcTimestamp) {
        BigDecimal currentDerivedBalance = Profile.getCurrent().dataSource().mapRepo(
                AccountRepository.class,
                repo -> repo.deriveBalance(account.id, utcTimestamp.toInstant(ZoneOffset.UTC))
        );
        if (!reportedBalance.setScale(currentDerivedBalance.scale(), RoundingMode.HALF_UP).equals(currentDerivedBalance)) {
            String msg = "The balance you reported (%s) doesn't match the balance that Perfin derived from your account's transactions (%s). It's encouraged to go back and add any missing transactions first, but you may proceed now if you understand the consequences of an inconsistent account balance history.\n\nAre you absolutely sure you want to create this balance record?".formatted(
                    CurrencyUtil.formatMoney(new MoneyValue(reportedBalance, account.getCurrency())),
                    CurrencyUtil.formatMoney(new MoneyValue(currentDerivedBalance, account.getCurrency()))
            );
            return Popups.confirm(timestampField, msg);
        }
        return true;
    }
}
