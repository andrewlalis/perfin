package com.andrewlalis.perfin.model;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * An amount of money of a certain currency.
 * @param amount The amount of money.
 * @param currency The currency of the money.
 */
public record MoneyValue(BigDecimal amount, Currency currency) {
    public static MoneyValue from(String amountStr, String currencyCode) {
        return new MoneyValue(new BigDecimal(amountStr), Currency.getInstance(currencyCode));
    }
}
