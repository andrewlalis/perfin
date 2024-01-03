package com.andrewlalis.perfin.data.util;

import com.andrewlalis.perfin.model.MoneyValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;

public class CurrencyUtil {
    public static String formatMoney(MoneyValue money) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        nf.setCurrency(money.currency());
        nf.setMaximumFractionDigits(money.currency().getDefaultFractionDigits());
        nf.setMinimumFractionDigits(money.currency().getDefaultFractionDigits());
        BigDecimal displayValue = money.amount().setScale(money.currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return nf.format(displayValue);
    }

    public static String formatMoneyWithCurrencyPrefix(MoneyValue money) {
        return money.currency().getCurrencyCode() + ' ' + formatMoney(money);
    }

    public static String formatMoneyAsBasicNumber(MoneyValue money) {
        BigDecimal displayValue = money.amount().setScale(money.currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return displayValue.toString();
    }
}
