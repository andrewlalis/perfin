package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.util.DateUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class AccountHistoryTile extends VBox {
    public AccountHistoryTile(LocalDateTime timestamp, Node centerContent) {
        getStyleClass().add("history-tile");

        Label timestampLabel = new Label(DateUtil.formatUTCAsLocalWithZone(timestamp));
        timestampLabel.getStyleClass().addAll("small-font", "mono-font", "secondary-color-text-fill");
        getChildren().add(timestampLabel);
        getChildren().add(centerContent);
    }
}
