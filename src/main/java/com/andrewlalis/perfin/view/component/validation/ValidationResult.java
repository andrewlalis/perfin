package com.andrewlalis.perfin.view.component.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record ValidationResult(List<String> messages) {
    public boolean isValid() {
        return messages.isEmpty();
    }

    public String asLines() {
        return messages.stream().map(String::strip).collect(Collectors.joining("\n"));
    }

    public static ValidationResult valid() {
        return new ValidationResult(Collections.emptyList());
    }

    public static ValidationResult of(String... messages) {
        return new ValidationResult(List.of(messages));
    }
}
