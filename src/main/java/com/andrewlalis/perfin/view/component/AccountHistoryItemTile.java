package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.AccountViewController;
import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * A tile that shows a brief bit of information about an account history item.
 */
public abstract class AccountHistoryItemTile extends BorderPane {
    public AccountHistoryItemTile(AccountHistoryItem item) {
        getStyleClass().add("tile");

        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(item.getTimestamp()));
        timestampLabel.getStyleClass().add("small-font");
        setTop(timestampLabel);
    }

    public static AccountHistoryItemTile forItem(
            AccountHistoryItem item,
            AccountHistoryItemRepository repo,
            AccountViewController controller
    ) {
        return switch (item.getType()) {
            case TEXT -> new AccountHistoryTextTile(item, repo);
            case ACCOUNT_ENTRY -> new AccountHistoryAccountEntryTile(item, repo);
            case BALANCE_RECORD -> new AccountHistoryBalanceRecordTile(item, repo, controller);
        };
    }
}
