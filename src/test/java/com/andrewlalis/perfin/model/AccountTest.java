package com.andrewlalis.perfin.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountTest {
    @Test
    public void testGetAccountNumberSuffix() {
        assertEquals("...1234", getTestAccountWithNumber("4328-1234").getAccountNumberSuffix());
    }

    @Test
    public void testGetAccountNumberGrouped() {
        assertEquals(
                "1234-5678-9101-1121",
                getTestAccountWithNumber("1234567891011121")
                        .getAccountNumberGrouped(4, '-')
        );
        assertEquals(
                "123-456-7",
                getTestAccountWithNumber("1234567")
                        .getAccountNumberGrouped(3, '-')
        );
    }

    private Account getTestAccountWithNumber(String num) {
        return new Account(
                1,
                LocalDateTime.now(),
                false,
                AccountType.CHECKING,
                num,
                "Testing Account",
                Currency.getInstance("USD")
        );
    }
}
