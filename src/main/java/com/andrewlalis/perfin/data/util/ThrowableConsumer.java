package com.andrewlalis.perfin.data.util;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    void accept(T value) throws Exception;
}
