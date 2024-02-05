package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.util.ColorUtil;
import com.andrewlalis.perfin.model.Profile;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract dashboard module for displaying a pie chart of data based on a
 * selected currency context.
 */
public abstract class PieChartModule extends DashboardModule {
    private final ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
    protected final List<Color> dataColors = new ArrayList<>();
    private final ChoiceBox<Currency> currencyChoiceBox = new ChoiceBox<>();
    private final String preferredCurrencySetting;

    public PieChartModule(Pane parent, String title, String preferredCurrencySetting) {
        super(parent);
        this.preferredCurrencySetting = preferredCurrencySetting;
        PieChart chart = new PieChart(chartData);
        chart.setLegendVisible(false);
        this.getChildren().add(new ModuleHeader(
                title,
                currencyChoiceBox
        ));
        this.getChildren().add(chart);
        currencyChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                getChartData(newValue).exceptionally(throwable -> {
                    throwable.printStackTrace(System.err);
                    return Collections.emptyList();
                })
                .thenAccept(data -> Platform.runLater(() -> {
                    chartData.setAll(data);
                    if (!dataColors.isEmpty()) {
                        for (int i = 0; i < dataColors.size(); i++) {
                            if (i >= data.size()) break;
                            data.get(i).getNode().setStyle("-fx-pie-color: #" + ColorUtil.toHex(dataColors.get(i)));
                        }
                    }
                }));
                Profile.getCurrent().setSettingAndSave(preferredCurrencySetting, newValue.getCurrencyCode());
            } else {
                chartData.clear();
            }
        });
    }

    @Override
    public void refreshContents() {
        refreshCurrencies();
    }

    private void refreshCurrencies() {
        Profile.getCurrent().dataSource().mapRepoAsync(
                AccountRepository.class,
                AccountRepository::findAllUsedCurrencies
        )
            .thenAccept(currencies -> {
                final List<Currency> orderedCurrencies = currencies.isEmpty()
                        ? List.of(Currency.getInstance("USD"))
                        : currencies.stream()
                            .sorted(Comparator.comparing(Currency::getCurrencyCode))
                            .toList();
                final Currency preferredCurrency = Profile.getCurrent().getSetting(preferredCurrencySetting)
                        .map(Currency::getInstance).orElse(null);
                Platform.runLater(() -> {
                    currencyChoiceBox.getItems().setAll(orderedCurrencies);
                    if (preferredCurrency != null && currencies.contains(preferredCurrency)) {
                        currencyChoiceBox.getSelectionModel().select(preferredCurrency);
                    } else {
                        currencyChoiceBox.getSelectionModel().selectFirst();
                    }
                });
            });
    }

    protected abstract CompletableFuture<List<PieChart.Data>> getChartData(Currency currency);
}
