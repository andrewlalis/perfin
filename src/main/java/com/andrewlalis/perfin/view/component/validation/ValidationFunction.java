package com.andrewlalis.perfin.view.component.validation;

public interface ValidationFunction<T> {
    ValidationResult validate(T input);
}
