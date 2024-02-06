package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.control.TransactionsViewController;
import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.DataSource;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.data.util.DateUtil;
import com.andrewlalis.perfin.model.AccountEntry;
import com.andrewlalis.perfin.model.BalanceRecord;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.Timestamped;
import com.andrewlalis.perfin.model.history.HistoryTextItem;
import com.andrewlalis.perfin.view.BindingUtil;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.util.List;

import static com.andrewlalis.perfin.PerfinApp.router;

public class AccountHistoryView extends ScrollPane {
    private LocalDateTime lastTimestamp = null;
    private final BooleanProperty canLoadMore = new SimpleBooleanProperty(true);
    private final VBox itemsVBox = new VBox();
    private final LongProperty accountIdProperty = new SimpleLongProperty(-1L);
    private final IntegerProperty initialItemsToLoadProperty = new SimpleIntegerProperty(10);

    public AccountHistoryView() {
        VBox scrollableContentVBox = new VBox();
        scrollableContentVBox.getChildren().add(itemsVBox);
        itemsVBox.setMinWidth(0);

        Hyperlink loadMoreLink = new Hyperlink("Load more history");
        loadMoreLink.setOnAction(event -> loadMoreHistory());
        BindingUtil.bindManagedAndVisible(loadMoreLink, canLoadMore);

        scrollableContentVBox.getChildren().add(new BorderPane(loadMoreLink));
        itemsVBox.getStyleClass().addAll("tile-container");
        this.setContent(scrollableContentVBox);
        this.setFitToHeight(true);
        this.setFitToWidth(true);
        this.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        this.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    }

    public void loadMoreHistory() {
        long accountId = accountIdProperty.get();
        int maxItems = initialItemsToLoadProperty.get();
        DataSource ds = Profile.getCurrent().dataSource();
        ds.mapRepoAsync(AccountRepository.class, repo -> repo.findEventsBefore(accountId, lastTimestamp(), maxItems))
                .thenAccept(entities -> Platform.runLater(() -> addEntitiesToHistory(entities, maxItems)));
    }

    public void clear() {
        itemsVBox.getChildren().clear();
        canLoadMore.set(true);
        lastTimestamp = null;
    }

    public void setAccountId(long accountId) {
        this.accountIdProperty.set(accountId);
    }

    // Property methods
    public final IntegerProperty initialItemsToLoadProperty() {
        return initialItemsToLoadProperty;
    }

    public final int getInitialItemsToLoad() {
        return initialItemsToLoadProperty.get();
    }

    public final void setInitialItemsToLoad(int value) {
        initialItemsToLoadProperty.set(value);
    }

    private LocalDateTime lastTimestamp() {
        if (lastTimestamp == null) return DateUtil.nowAsUTC();
        return lastTimestamp;
    }

    private Node makeTile(Timestamped entity) {
        switch (entity) {
            case HistoryTextItem textItem -> {
                return new AccountHistoryTile(textItem.getTimestamp(), new TextFlow(new Text(textItem.getDescription())));
            }
            case AccountEntry ae -> {
                Hyperlink txLink = new Hyperlink("Transaction #" + ae.getTransactionId());
                txLink.setOnAction(event -> router.navigate("transactions", new TransactionsViewController.RouteContext(ae.getTransactionId())));
                String descriptionFormat = ae.getType() == AccountEntry.Type.CREDIT
                        ? "credited %s from this account."
                        : "debited %s to this account.";
                String description = descriptionFormat.formatted(CurrencyUtil.formatMoney(ae.getMoneyValue()));
                TextFlow textFlow = new TextFlow(txLink, new Text(description));
                return new AccountHistoryTile(ae.getTimestamp(), textFlow);
            }
            case BalanceRecord br -> {
                Hyperlink brLink = new Hyperlink("Balance Record #" + br.id);
                brLink.setOnAction(event -> router.navigate("balance-record", br));
                return new AccountHistoryTile(br.getTimestamp(), new TextFlow(
                        brLink,
                        new Text("added with a value of %s.".formatted(CurrencyUtil.formatMoney(br.getMoneyAmount())))
                ));
            }
            default -> {
                return new AccountHistoryTile(entity.getTimestamp(), new TextFlow(new Text("Unsupported entity: " + entity.getClass().getName())));
            }
        }
    }

    private void addEntitiesToHistory(List<Timestamped> entities, int requestedItems) {
        if (!itemsVBox.getChildren().isEmpty()) {
            itemsVBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        }
        itemsVBox.getChildren().addAll(entities.stream()
                .map(this::makeTile)
                .map(tile -> {
                    // Use this to scrunch content to the left.
                    AnchorPane ap = new AnchorPane(tile);
                    AnchorPane.setLeftAnchor(tile, 0.0);
                    return ap;
                })
                .toList());
        if (entities.size() < requestedItems) {
            canLoadMore.set(false);
            BorderPane endMarker = new BorderPane(new Label("This is the start of the history."));
            endMarker.getStyleClass().addAll("large-font", "italic-text");
            itemsVBox.getChildren().add(endMarker);
        }
        if (!entities.isEmpty()) {
            lastTimestamp = entities.getLast().getTimestamp();
        }
    }
}
