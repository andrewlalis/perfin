package com.andrewlalis.perfin.data.pagination;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public record Page<T>(List<T> items, PageRequest pagination) {
    public Stream<T> stream() {
        return items.stream();
    }

    public <U> Page<U> map(Function<T, U> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), pagination);
    }
}
