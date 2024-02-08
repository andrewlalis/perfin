package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.data.AnalyticsRepository;
import com.andrewlalis.perfin.data.TimestampRange;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionCategory;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpendingCategoryChartModule extends PieChartModule {
    public SpendingCategoryChartModule(Pane parent) {
        super(
                parent,
                "Spending by Category",
                "charts.category-spend.default-currency",
                "charts.category-spend.default-time-range"
        );
    }

    @Override
    protected CompletableFuture<List<PieChart.Data>> getChartData(Currency currency, TimestampRange range) {
        return Profile.getCurrent().dataSource().mapRepoAsync(AnalyticsRepository.class, repo -> {
            var data = repo.getSpendByRootCategory(range, currency);
            dataColors.clear();
            return data.stream()
                    .map(pair -> {
                        TransactionCategory category = pair.first();
                        BigDecimal amount = pair.second();
                        String label = category == null ? "Uncategorized" : category.getName();
                        label += ": " + CurrencyUtil.formatMoney(new MoneyValue(amount, currency));
                        var datum = new PieChart.Data(label, amount.doubleValue());
                        Color color = category == null ? Color.GRAY : category.getColor();
                        dataColors.add(color);
                        return datum;
                    })
                    .toList();
        });
    }
}
