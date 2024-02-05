package com.andrewlalis.perfin.view.component.module;

import javafx.scene.chart.PieChart;
import javafx.scene.layout.Pane;

public class SpendingCategoryChartModule extends DashboardModule {
    public SpendingCategoryChartModule(Pane parent) {
        super(parent);
        PieChart chart = new PieChart();
    }

    @Override
    public void refreshContents() {

    }
}
