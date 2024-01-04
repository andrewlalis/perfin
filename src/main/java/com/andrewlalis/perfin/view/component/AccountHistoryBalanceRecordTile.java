package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.AccountViewController;
import com.andrewlalis.perfin.control.Popups;
import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class AccountHistoryBalanceRecordTile extends AccountHistoryItemTile {
    public AccountHistoryBalanceRecordTile(AccountHistoryItem item, AccountHistoryItemRepository repo, AccountViewController controller) {
        super(item);
        BalanceRecord balanceRecord = repo.getBalanceRecordItem(item.id);
        if (balanceRecord == null) {
            setCenter(new TextFlow(new Text("Deleted balance record was added.")));
            return;
        }

        Text amountText = new Text(CurrencyUtil.formatMoneyWithCurrencyPrefix(balanceRecord.getMoneyAmount()));
        var text = new TextFlow(new Text("Balance record #" + balanceRecord.id + " added with value of "), amountText);
        setCenter(text);

        Hyperlink deleteLink = new Hyperlink("Delete this balance record");
        deleteLink.setOnAction(event -> {
            boolean confirm = Popups.confirm("Are you sure you want to delete this balance record? It will be removed permanently, and cannot be undone.");
            if (confirm) {
                Profile.getCurrent().getDataSource().useBalanceRecordRepository(balanceRecordRepo -> {
                    balanceRecordRepo.deleteById(balanceRecord.id);
                    Platform.runLater(controller::reloadHistory);
                });
            }
        });
        setBottom(deleteLink);
    }
}
