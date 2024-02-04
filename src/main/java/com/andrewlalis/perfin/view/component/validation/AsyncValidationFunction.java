package com.andrewlalis.perfin.view.component.validation;

import java.util.concurrent.CompletableFuture;

public interface AsyncValidationFunction<T> {
    CompletableFuture<ValidationResult> validate(T input);
}
