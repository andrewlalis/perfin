package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.model.history.HistoryTextItem;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class AccountHistoryTextTile extends AccountHistoryItemTile {
    public AccountHistoryTextTile(HistoryTextItem item) {
        super(item);
        setCenter(new TextFlow(new Text(item.getDescription())));
    }
}
