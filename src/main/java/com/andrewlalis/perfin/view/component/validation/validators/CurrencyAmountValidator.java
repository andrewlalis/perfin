package com.andrewlalis.perfin.view.component.validation.validators;

import com.andrewlalis.perfin.view.component.validation.ValidationFunction;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.function.Supplier;

public class CurrencyAmountValidator implements ValidationFunction<String> {
    private final Supplier<Currency> currencySupplier;
    private final boolean allowNegative;
    private final boolean allowEmpty;

    public CurrencyAmountValidator(Supplier<Currency> currencySupplier, boolean allowNegative, boolean allowEmpty) {
        this.currencySupplier = currencySupplier;
        this.allowNegative = allowNegative;
        this.allowEmpty = allowEmpty;
    }

    @Override
    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            if (!allowEmpty) {
                return ValidationResult.of("Amount should not be empty.");
            }
            return ValidationResult.valid();
        }

        try {
            BigDecimal amount = new BigDecimal(input);
            int scale = amount.scale();
            List<String> messages = new ArrayList<>();
            Currency currency = currencySupplier.get();
            if (currency != null && scale > currency.getDefaultFractionDigits()) {
                messages.add("The selected currency doesn't support that many digits.");
            }
            if (!allowNegative && amount.compareTo(BigDecimal.ZERO) < 0) {
                messages.add("Negative amounts are not allowed.");
            }
            return new ValidationResult(messages);
        } catch (NumberFormatException e) {
            return ValidationResult.of("Invalid amount. Should be a decimal value.");
        }
    }
}
