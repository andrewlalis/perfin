package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.TransactionsViewController;
import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountHistoryAccountEntryTile extends AccountHistoryItemTile {
    public AccountHistoryAccountEntryTile(AccountHistoryItem item, AccountHistoryItemRepository repo) {
        super(item);
        AccountEntry entry = repo.getAccountEntryItem(item.getId());
        if (entry == null) {
            setCenter(new TextFlow(new Text("Deleted account entry because of deleted transaction.")));
            return;
        }

        Text amountText = new Text(CurrencyUtil.formatMoneyWithCurrencyPrefix(entry.getMoneyValue()));
        Hyperlink transactionLink = new Hyperlink("Transaction #" + entry.getTransactionId());
        transactionLink.setOnAction(event -> router.navigate(
                "transactions",
                new TransactionsViewController.RouteContext(entry.getTransactionId())
        ));
        var text = new TextFlow(
                transactionLink,
                new Text("posted as a " + entry.getType().name().toLowerCase() + " to this account, with a value of "),
                amountText
        );
        setCenter(text);
    }
}
