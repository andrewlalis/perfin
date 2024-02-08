package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.data.AccountRepository;
import com.andrewlalis.perfin.data.TimestampRange;
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
import java.util.function.Supplier;

/**
 * An abstract dashboard module for displaying a pie chart of data based on a
 * selected currency context and time window.
 */
public abstract class PieChartModule extends DashboardModule {
    private static final Map<String, Supplier<TimestampRange>> TIMESTAMP_RANGES = Map.of(
            "Last 7 days", () -> TimestampRange.lastNDays(7),
            "Last 30 days", () -> TimestampRange.lastNDays(30),
            "Last 90 days", () -> TimestampRange.lastNDays(90),
            "This Month", TimestampRange::thisMonth,
            "This Year", TimestampRange::thisYear,
            "All Time", TimestampRange::unbounded
    );
    private static final String[] RANGE_CHOICES = {
            "Last 7 days",
            "Last 30 days",
            "Last 90 days",
            "This Month",
            "This Year",
            "All Time"
    };

    private final ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
    protected final List<Color> dataColors = new ArrayList<>();
    private final ChoiceBox<Currency> currencyChoiceBox = new ChoiceBox<>();
    private final ChoiceBox<String> timeRangeChoiceBox = new ChoiceBox<>();
    private final String preferredCurrencySetting;
    private final String timeRangeSetting;

    public PieChartModule(Pane parent, String title, String preferredCurrencySetting, String timeRangeSetting) {
        super(parent);
        this.preferredCurrencySetting = preferredCurrencySetting;
        this.timeRangeSetting = timeRangeSetting;

        this.timeRangeChoiceBox.getItems().addAll(RANGE_CHOICES);
        this.timeRangeChoiceBox.getSelectionModel().select("All Time");

        PieChart chart = new PieChart(chartData);
        chart.setLegendVisible(false);
        this.getChildren().add(new ModuleHeader(
                title,
                timeRangeChoiceBox,
                currencyChoiceBox
        ));
        this.getChildren().add(chart);
        currencyChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                renderChart();
            } else {
                chartData.clear();
            }
        });
        timeRangeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            renderChart();
        });
    }

    @Override
    public void refreshContents() {
        refreshCurrencies();
        String savedTimeRangeLabel = Profile.getCurrent().getSetting(timeRangeSetting).orElse(null);
        if (savedTimeRangeLabel != null && TIMESTAMP_RANGES.containsKey(savedTimeRangeLabel)) {
            timeRangeChoiceBox.getSelectionModel().select(savedTimeRangeLabel);
        }
    }

    private TimestampRange getSelectedTimestampRange() {
        String selectedLabel = timeRangeChoiceBox.getValue();
        if (selectedLabel == null || !TIMESTAMP_RANGES.containsKey(selectedLabel)) {
            return TimestampRange.unbounded();
        }
        return TIMESTAMP_RANGES.get(selectedLabel).get();
    }

    private void renderChart() {
        final Currency currency = currencyChoiceBox.getValue();
        String timeRangeLabel = timeRangeChoiceBox.getValue();
        if (currency == null || timeRangeLabel == null) {
            chartData.clear();
            dataColors.clear();
            return;
        }
        final TimestampRange range = getSelectedTimestampRange();
        getChartData(currency, range).exceptionally(throwable -> {
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
        Profile.getCurrent().setSettingAndSave(preferredCurrencySetting, currency.getCurrencyCode());
        Profile.getCurrent().setSettingAndSave(timeRangeSetting, timeRangeLabel);
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

    protected abstract CompletableFuture<List<PieChart.Data>> getChartData(Currency currency, TimestampRange range);
}
