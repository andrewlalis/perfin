package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.history.HistoryItem;
import com.andrewlalis.perfin.model.history.HistoryTextItem;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * A tile that shows a brief bit of information about an account history item.
 */
public abstract class AccountHistoryItemTile extends BorderPane {
    public AccountHistoryItemTile(HistoryItem item) {
        getStyleClass().add("tile");

        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(item.getTimestamp()));
        timestampLabel.getStyleClass().add("small-font");
        setTop(timestampLabel);
    }

    public static AccountHistoryItemTile forItem(
            HistoryItem item
    ) {
        if (item instanceof HistoryTextItem t) {
            return new AccountHistoryTextTile(t);
        }
        throw new RuntimeException("Unsupported history item type: " + item.getType());
    }
}
