package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import com.andrewlalis.perfin.view.component.PropertiesPane;
import com.andrewlalis.perfin.view.component.validation.ValidationApplier;
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
import java.time.format.DateTimeParseException;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateBalanceRecordController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public TextField balanceField;
    @FXML public Label balanceWarningLabel;
    private FileSelectionArea attachmentSelectionArea;
    @FXML public PropertiesPane propertiesPane;

    @FXML public Button saveButton;

    private Account account;

    @FXML public void initialize() {
        var timestampValid = new ValidationApplier<String>(input -> {
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
            if (!balanceValidator.validate(newValue).isValid()) {
                balanceWarningLabel.visibleProperty().set(false);
                return;
            }
            BigDecimal reportedBalance = new BigDecimal(newValue);
            Thread.ofVirtual().start(() -> Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                BigDecimal derivedBalance = repo.deriveCurrentBalance(account.id);
                Platform.runLater(() -> balanceWarningLabel.visibleProperty().set(
                        !reportedBalance.setScale(derivedBalance.scale(), RoundingMode.HALF_UP).equals(derivedBalance)
                ));
            }));
        });

        var formValid = timestampValid.and(balanceValid);
        saveButton.disableProperty().bind(formValid.not());

        // Manually append the attachment selection area to the end of the properties pane.
        attachmentSelectionArea = new FileSelectionArea(
                FileUtil::newAttachmentsFileChooser,
                () -> timestampField.getScene().getWindow()
        );
        attachmentSelectionArea.allowMultiple.set(true);
        propertiesPane.getChildren().addLast(attachmentSelectionArea);
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
        Thread.ofVirtual().start(() -> Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
            BigDecimal value = repo.deriveCurrentBalance(account.id);
            Platform.runLater(() -> balanceField.setText(
                    CurrencyUtil.formatMoneyAsBasicNumber(new MoneyValue(value, account.getCurrency()))
            ));
        }));
        attachmentSelectionArea.clear();
    }

    @FXML public void save() {
        LocalDateTime localTimestamp = LocalDateTime.parse(timestampField.getText(), DateUtil.DEFAULT_DATETIME_FORMAT);
        BigDecimal reportedBalance = new BigDecimal(balanceField.getText());

        boolean confirm = Popups.confirm("Are you sure that you want to record the balance of account\n%s\nas %s,\nas of %s?".formatted(
                account.getShortName(),
                CurrencyUtil.formatMoneyWithCurrencyPrefix(new MoneyValue(reportedBalance, account.getCurrency())),
                localTimestamp.atZone(ZoneId.systemDefault()).format(DateUtil.DEFAULT_DATETIME_FORMAT_WITH_ZONE)
        ));
        if (confirm && confirmIfInconsistentBalance(reportedBalance)) {
            Profile.getCurrent().getDataSource().useBalanceRecordRepository(repo -> {
                repo.insert(
                        DateUtil.localToUTC(localTimestamp),
                        account.id,
                        reportedBalance,
                        account.getCurrency(),
                        attachmentSelectionArea.getSelectedFiles()
                );
            });
            router.navigateBackAndClear();
        }
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }

    private boolean confirmIfInconsistentBalance(BigDecimal reportedBalance) {
        BigDecimal currentDerivedBalance;
        try (var accountRepo = Profile.getCurrent().getDataSource().getAccountRepository()) {
            currentDerivedBalance = accountRepo.deriveCurrentBalance(account.id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!reportedBalance.setScale(currentDerivedBalance.scale(), RoundingMode.HALF_UP).equals(currentDerivedBalance)) {
            String msg = "The balance you reported (%s) doesn't match the balance that Perfin derived from your account's transactions (%s). It's encouraged to go back and add any missing transactions first, but you may proceed now if you understand the consequences of an inconsistent account balance history.\n\nAre you absolutely sure you want to create this balance record?".formatted(
                    CurrencyUtil.formatMoney(new MoneyValue(reportedBalance, account.getCurrency())),
                    CurrencyUtil.formatMoney(new MoneyValue(currentDerivedBalance, account.getCurrency()))
            );
            return Popups.confirm(msg);
        }
        return true;
    }
}
