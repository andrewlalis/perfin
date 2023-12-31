package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A tile that shows a brief bit of information about an account history item.
 */
public class AccountHistoryItemTile extends BorderPane {
    public AccountHistoryItemTile(AccountHistoryItem item, AccountHistoryItemRepository repo) {
        setStyle("""
                -fx-border-color: lightgray;
                -fx-border-radius: 5px;
                -fx-padding: 5px;
                """);

        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(item.getTimestamp()));
        timestampLabel.setStyle("-fx-font-size: small;");
        setTop(timestampLabel);
        setCenter(switch (item.getType()) {
            case TEXT -> buildTextItem(repo.getTextItem(item.getId()));
            case ACCOUNT_ENTRY -> buildAccountEntryItem(repo.getAccountEntryItem(item.getId()));
            case BALANCE_RECORD -> buildBalanceRecordItem(repo.getBalanceRecordItem(item.getId()));
        });
    }

    private Node buildTextItem(String text) {
        return new TextFlow(new Text(text));
    }

    private Node buildAccountEntryItem(AccountEntry entry) {
        Text amountText = new Text(CurrencyUtil.formatMoney(entry.getSignedAmount(), entry.getCurrency()));
        Hyperlink transactionLink = new Hyperlink("Transaction #" + entry.getTransactionId());
        return new TextFlow(
                new Text("Entry added with value of "),
                amountText,
                new Text(", linked with "),
                transactionLink,
                new Text(".")
        );
    }

    private Node buildBalanceRecordItem(BalanceRecord balanceRecord) {
        Text amountText = new Text(CurrencyUtil.formatMoney(balanceRecord.getBalance(), balanceRecord.getCurrency()));
        return new TextFlow(new Text("Balance record added with value of "), amountText);
    }
}
