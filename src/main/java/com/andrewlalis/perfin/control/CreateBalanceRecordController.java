package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.data.util.FileUtil;
import com.andrewlalis.perfin.model.Account;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.FileSelectionArea;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.andrewlalis.perfin.PerfinApp.router;

public class CreateBalanceRecordController implements RouteSelectionListener {
    @FXML public TextField timestampField;
    @FXML public TextField balanceField;
    @FXML public VBox attachmentsVBox;
    private FileSelectionArea attachmentSelectionArea;

    private Account account;

    @FXML public void initialize() {
        attachmentSelectionArea = new FileSelectionArea(FileUtil::newAttachmentsFileChooser, () -> attachmentsVBox.getScene().getWindow());
        attachmentSelectionArea.allowMultiple.set(true);
        attachmentsVBox.getChildren().add(attachmentSelectionArea);
    }

    @Override
    public void onRouteSelected(Object context) {
        this.account = (Account) context;
        timestampField.setText(LocalDateTime.now().format(DateUtil.DEFAULT_DATETIME_FORMAT));
        Thread.ofVirtual().start(() -> {
            Profile.getCurrent().getDataSource().useAccountRepository(repo -> {
                BigDecimal value = repo.deriveCurrentBalance(account.getId());
                Platform.runLater(() -> balanceField.setText(
                        CurrencyUtil.formatMoneyAsBasicNumber(new MoneyValue(value, account.getCurrency()))
                ));
            });
        });
        attachmentSelectionArea.clear();
    }

    @FXML public void save() {
        // TODO: Add validation.
        Profile.getCurrent().getDataSource().useBalanceRecordRepository(repo -> {
            LocalDateTime localTimestamp = LocalDateTime.parse(timestampField.getText(), DateUtil.DEFAULT_DATETIME_FORMAT);
            BigDecimal reportedBalance = new BigDecimal(balanceField.getText());
            repo.insert(
                    DateUtil.localToUTC(localTimestamp),
                    account.getId(),
                    reportedBalance,
                    account.getCurrency(),
                    attachmentSelectionArea.getSelectedFiles()
            );
        });
        router.navigateBackAndClear();
    }

    @FXML public void cancel() {
        router.navigateBackAndClear();
    }
}
