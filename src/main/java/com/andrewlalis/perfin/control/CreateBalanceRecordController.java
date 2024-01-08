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
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateBalanceRecordController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public TextField balanceField;
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

        var balanceValid = new ValidationApplier<>(
                new CurrencyAmountValidator(() -> account == null ? null : account.getCurrency(), true, false)
        ).validatedInitially().attachToTextField(balanceField);

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
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                BigDecimal value = repo.deriveCurrentBalance(account.id);
                Platform.runLater(() -> balanceField.setText(
                        CurrencyUtil.formatMoneyAsBasicNumber(new MoneyValue(value, account.getCurrency()))
                ));
            });
        });
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
        if (confirm) {
            Profile.getCurrent().getDataSource().useAccountRepository(accountRepo -> {
                BigDecimal currentDerivedBalance = accountRepo.deriveCurrentBalance(account.id);

            });
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
}
