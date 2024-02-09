package com.andrewlalis.perfin.view.component;

import com.andrewlalis.perfin.data.TransactionCategoryRepository;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionCategory;
import com.andrewlalis.perfin.model.TransactionLineItem;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TransactionLineItemTile extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(TransactionLineItemTile.class);

    private TransactionLineItemTile() {}

    public static CompletableFuture<TransactionLineItemTile> build(TransactionLineItem item, ObservableValue<Currency> currencyValue, List<TransactionCategory> categoriesCache) {
        TransactionLineItemTile tile = new TransactionLineItemTile();
        tile.getStyleClass().addAll("std-spacing", "std-padding", "small-font");
        tile.setStyle("-fx-background-color: -fx-theme-background-2;");
        Function<String, Label> boldLabelMaker = s -> {
            Label lbl = new Label(s);
            lbl.getStyleClass().addAll("bold-text");
            return lbl;
        };
        Label descriptionLabel = new Label(item.getDescription());
        Label valuePerItemLabel = new Label();
        valuePerItemLabel.getStyleClass().add("mono-font");
        valuePerItemLabel.textProperty().bind(currencyValue
                .map(currency -> CurrencyUtil.formatMoney(new MoneyValue(item.getValuePerItem(), currency)))
        );
        Label totalValueLabel = new Label();
        totalValueLabel.getStyleClass().add("mono-font");
        totalValueLabel.textProperty().bind(currencyValue
                .map(currency -> CurrencyUtil.formatMoney(new MoneyValue(item.getTotalValue(), currency)))
        );
        Label quantityLabel = new Label(Integer.toString(item.getQuantity()));
        quantityLabel.getStyleClass().add("mono-font");
        PropertiesPane propertiesPane = new PropertiesPane(80);
        propertiesPane.getChildren().addAll(
                boldLabelMaker.apply("Description"), descriptionLabel,
                boldLabelMaker.apply("Quantity"), quantityLabel,
                boldLabelMaker.apply("Item Value"), valuePerItemLabel,
                boldLabelMaker.apply("Total"), totalValueLabel
        );
        tile.setCenter(propertiesPane);
        if (item.getCategoryId() != null) {
            if (categoriesCache != null) {
                TransactionCategory category = categoriesCache.stream()
                        .filter(c -> c.id == item.getCategoryId())
                        .findFirst().orElse(null);
                if (category == null) {
                    log.warn("Failed to find cached category for line item.");
                } else {
                    propertiesPane.getChildren().addAll(
                            boldLabelMaker.apply("Category"), new CategoryLabel(category, 5)
                    );
                }
                return CompletableFuture.completedFuture(tile);
            } else {
                CompletableFuture<TransactionLineItemTile> cf = new CompletableFuture<>();
                Profile.getCurrent().dataSource().mapRepoAsync(
                        TransactionCategoryRepository.class,
                        repo -> repo.findById(item.getCategoryId()).orElse(null)
                ).thenAccept(category -> Platform.runLater(() -> {
                    propertiesPane.getChildren().addAll(
                            boldLabelMaker.apply("Category"), new CategoryLabel(category, 5)
                    );
                    cf.complete(tile);
                }));
                return cf;
            }
        } else {
            return CompletableFuture.completedFuture(tile);
        }
    }
}
