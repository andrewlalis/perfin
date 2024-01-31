package com.andrewlalis.perfin.view.component.validation.validators;

import com.andrewlalis.perfin.view.component.validation.AsyncValidationFunction;
import com.andrewlalis.perfin.view.component.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * A common validator pattern, where a series of checks are done on a value to
 * determine if it's valid. If invalid, a message is added.
 * @param <T> The value type.
 */
public class PredicateValidator<T> implements AsyncValidationFunction<T> {
    private static final Logger logger = LoggerFactory.getLogger(PredicateValidator.class);

    private record ValidationStep<T>(Function<T, CompletableFuture<Boolean>> predicate, String message, boolean terminal) {}

    private final List<ValidationStep<T>> steps = new ArrayList<>();

    private PredicateValidator<T> addPredicate(Function<T, Boolean> predicate, String errorMessage, boolean terminal) {
        steps.add(new ValidationStep<>(
                v -> CompletableFuture.completedFuture(predicate.apply(v)),
                errorMessage,
                terminal
        ));
        return this;
    }

    private PredicateValidator<T> addAsyncPredicate(Function<T, CompletableFuture<Boolean>> asyncPredicate, String errorMessage, boolean terminal) {
        steps.add(new ValidationStep<>(asyncPredicate, errorMessage, terminal));
        return this;
    }

    public PredicateValidator<T> addPredicate(Function<T, Boolean> predicate, String errorMessage) {
        return addPredicate(predicate, errorMessage, false);
    }

    public PredicateValidator<T> addAsyncPredicate(Function<T, CompletableFuture<Boolean>> asyncPredicate, String errorMessage) {
        return addAsyncPredicate(asyncPredicate, errorMessage, false);
    }

    /**
     * Adds a terminal predicate, that is, if the given boolean function
     * evaluates to false, then no further predicates are evaluated.
     * @param predicate The predicate function.
     * @param errorMessage The error message to display if the predicate
     *                     evaluates to false for a given value.
     * @return A reference to the validator, for method chaining.
     */
    public PredicateValidator<T> addTerminalPredicate(Function<T, Boolean> predicate, String errorMessage) {
        return addPredicate(predicate, errorMessage, true);
    }

    public PredicateValidator<T> addTerminalAsyncPredicate(Function<T, CompletableFuture<Boolean>> asyncPredicate, String errorMessage) {
        return addAsyncPredicate(asyncPredicate, errorMessage);
    }

    @Override
    public CompletableFuture<ValidationResult> validate(T input) {
        CompletableFuture<ValidationResult> cf = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            List<String> messages = new ArrayList<>();
            for (var step : steps) {
                try {
                    boolean success = step.predicate().apply(input).get();
                    if (!success) {
                        messages.add(step.message());
                        if (step.terminal()) {
                            cf.complete(new ValidationResult(messages));
                            return; // Exit if this is a terminal step and it failed.
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Applying a predicate to input failed.", e);
                    cf.completeExceptionally(e);
                }
            }
            cf.complete(new ValidationResult(messages));
        });
        return cf;
    }
}
