package com.andrewlalis.perfin.view.component.validation.validators;

import com.andrewlalis.perfin.view.component.validation.ValidationFunction;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A common validator pattern, where a series of checks are done on a value to
 * determine if it's valid. If invalid, a message is added.
 * @param <T> The value type.
 */
public class PredicateValidator<T> implements ValidationFunction<T> {
    private record ValidationStep<T>(Function<T, Boolean> predicate, String message, boolean terminal) {}

    private final List<ValidationStep<T>> steps = new ArrayList<>();

    public PredicateValidator<T> addPredicate(Function<T, Boolean> predicate, String errorMessage) {
        steps.add(new ValidationStep<>(predicate, errorMessage, false));
        return this;
    }

    public PredicateValidator<T> addTerminalPredicate(Function<T, Boolean> predicate, String errorMessage) {
        steps.add(new ValidationStep<>(predicate, errorMessage, true));
        return this;
    }

    @Override
    public ValidationResult validate(T input) {
        List<String> messages = new ArrayList<>();
        for (var step : steps) {
            if (!step.predicate().apply(input)) {
                messages.add(step.message());
                if (step.terminal()) {
                    return new ValidationResult(messages);
                }
            }
        }
        return new ValidationResult(messages);
    }
}
