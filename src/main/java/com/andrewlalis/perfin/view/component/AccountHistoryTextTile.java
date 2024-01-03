package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.AccountHistoryItemRepository;
import com.andrewlalis.perfin.model.history.AccountHistoryItem;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class AccountHistoryTextTile extends AccountHistoryItemTile {
    public AccountHistoryTextTile(AccountHistoryItem item, AccountHistoryItemRepository repo) {
        super(item);
        String text = repo.getTextItem(item.getId());
        setCenter(new TextFlow(new Text(text)));
    }
}
