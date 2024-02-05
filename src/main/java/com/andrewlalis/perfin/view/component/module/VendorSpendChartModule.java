package com.andrewlalis.perfin.view.component.module;

import com.andrewlalis.perfin.data.AnalyticsRepository;
import com.andrewlalis.perfin.data.TimestampRange;
import com.andrewlalis.perfin.data.util.CurrencyUtil;
import com.andrewlalis.perfin.model.MoneyValue;
import com.andrewlalis.perfin.model.Profile;
import com.andrewlalis.perfin.model.TransactionVendor;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VendorSpendChartModule extends PieChartModule {
    public VendorSpendChartModule(Pane parent) {
        super(parent, "Spending by Vendor", "charts.vendor-spend.default-currency");
    }

    @Override
    protected CompletableFuture<List<PieChart.Data>> getChartData(Currency currency) {
        return Profile.getCurrent().dataSource().mapRepoAsync(AnalyticsRepository.class, repo -> {
            var data = repo.getSpendByVendor(TimestampRange.unbounded(), currency);
            return data.stream()
                    .map(pair -> {
                        TransactionVendor vendor = pair.first();
                        BigDecimal amount = pair.second();
                        String label = vendor == null ? "Uncategorized" : vendor.getName();
                        label += ": " + CurrencyUtil.formatMoney(new MoneyValue(amount, currency));
                        return new PieChart.Data(label, amount.doubleValue());
                    })
                    .toList();
        });
    }
}
