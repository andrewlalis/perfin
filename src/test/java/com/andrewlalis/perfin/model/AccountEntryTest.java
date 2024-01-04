package com.andrewlalis.perfin.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AccountEntryTest {
    @Test
    public void testGetEffectiveValue() {
        // Debit entry on various accounts.
        AccountEntry debitEntry = getMockEntry(new BigDecimal("3.14"), AccountEntry.Type.DEBIT);
        assertEquals(new BigDecimal("3.14"), debitEntry.getEffectiveValue(AccountType.CHECKING));
        assertEquals(new BigDecimal("3.14"), debitEntry.getEffectiveValue(AccountType.SAVINGS));
        assertEquals(new BigDecimal("-3.14"), debitEntry.getEffectiveValue(AccountType.CREDIT_CARD));

        AccountEntry creditEntry = getMockEntry(new BigDecimal("1.23"), AccountEntry.Type.CREDIT);
        assertEquals(new BigDecimal("-1.23"), creditEntry.getEffectiveValue(AccountType.CHECKING));
        assertEquals(new BigDecimal("-1.23"), creditEntry.getEffectiveValue(AccountType.SAVINGS));
        assertEquals(new BigDecimal("1.23"), creditEntry.getEffectiveValue(AccountType.CREDIT_CARD));
    }

    private AccountEntry getMockEntry(BigDecimal amount, AccountEntry.Type type) {
        return new AccountEntry(
                1,
                LocalDateTime.of(2024, 1, 4, 9, 56),
                1,
                1,
                amount,
                type,
                Currency.getInstance("USD")
        );
    }
}
