package com.andrewlalis.perfin.control;

import com.andrewlalis.javafx_scene_router.RouteSelectionListener;
import com.andrewlalis.perfin.data.BalanceRecordRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.Attachment;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.view.component.AttachmentsViewPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

/**
 * Controller for the page which shows an overview of a balance record.
 */
public class BalanceRecordViewController implements RouteSelectionListener {
    private BalanceRecord balanceRecord;

    @FXML public Label titleLabel;

    @FXML public Label timestampLabel;
    @FXML public Label balanceLabel;
    @FXML public Label currencyLabel;
    @FXML public AttachmentsViewPane attachmentsViewPane;

    @FXML public void initialize() {
        attachmentsViewPane.hideIfEmpty();
    }

    @Override
    public void onRouteSelected(Object context) {
        this.balanceRecord = (BalanceRecord) context;
        if (balanceRecord == null) return;
        titleLabel.setText("Balance Record #" + balanceRecord.id);
        timestampLabel.setText(DateUtil.formatUTCAsLocalWithZone(balanceRecord.getTimestamp()));
        balanceLabel.setText(CurrencyUtil.formatMoney(balanceRecord.getMoneyAmount()));
        currencyLabel.setText(balanceRecord.getCurrency().getDisplayName());
        Profile.getCurrent().getDataSource().useRepoAsync(BalanceRecordRepository.class, repo -> {
            List<Attachment> attachments = repo.findAttachments(balanceRecord.id);
            Platform.runLater(() -> attachmentsViewPane.setAttachments(attachments));
        });
    }

    @FXML public void delete() {
        boolean confirm = Popups.confirm("Are you sure you want to delete this balance record? This may have an effect on the derived balance of your account, as shown in Perfin.");
        if (confirm) {
            Profile.getCurrent().getDataSource().useRepo(BalanceRecordRepository.class, repo -> repo.deleteById(balanceRecord.id));
            router.navigateBackAndClear();
        }
    }
}
