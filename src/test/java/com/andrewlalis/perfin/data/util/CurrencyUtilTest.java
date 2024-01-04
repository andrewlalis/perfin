package com.andrewlalis.perfin.data.util;

import com.andrewlalis.perfin.model.MoneyValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyUtilTest {
    @ParameterizedTest
    @MethodSource("testFormatMoneyParams")
    public void testFormatMoney(MoneyValue money, String expectedFormat) {
        assertEquals(expectedFormat, CurrencyUtil.formatMoney(money));
    }

    static Stream<Arguments> testFormatMoneyParams() {
        return Stream.of(
                Arguments.of(MoneyValue.from("1.23", "USD"), "$1.23"),
                Arguments.of(MoneyValue.from("1000", "USD"), "$1,000.00"),
                Arguments.of(MoneyValue.from("10000", "USD"), "$10,000.00"),
                Arguments.of(MoneyValue.from("0", "USD"), "$0.00"),
                Arguments.of(MoneyValue.from("-4.213", "USD"), "-$4.21"),
                Arguments.of(MoneyValue.from("5.6781", "USD"), "$5.68"),
                Arguments.of(MoneyValue.from("1.23", "EUR"), "€1.23"),
                Arguments.of(MoneyValue.from("212331", "JPY"), "¥212,331")
        );
    }

    @Test
    public void testFormatMoneyAsBasicNumber() {
        assertEquals("1.23", CurrencyUtil.formatMoneyAsBasicNumber(MoneyValue.from("1.23", "USD")));
        assertEquals("5438", CurrencyUtil.formatMoneyAsBasicNumber(MoneyValue.from("5438.213", "JPY")));
        assertEquals("0.00", CurrencyUtil.formatMoneyAsBasicNumber(MoneyValue.from("0", "USD")));
    }
}
