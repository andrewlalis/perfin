package com.andrewlalis.perfin.data.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;

public class CurrencyUtil {
    public static String formatMoney(BigDecimal amount, Currency currency) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        nf.setCurrency(currency);
        nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        nf.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        BigDecimal displayValue = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return nf.format(displayValue);
    }

    public static String formatMoneyAsBasicNumber(BigDecimal amount, Currency currency) {
        BigDecimal displayValue = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return displayValue.toString();
    }
}
