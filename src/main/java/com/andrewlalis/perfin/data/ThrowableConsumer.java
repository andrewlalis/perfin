package com.andrewlalis.perfin.data;

@FunctionalInterface
public interface ThrowableConsumer<T> {
    void accept(T value) throws Exception;
}
