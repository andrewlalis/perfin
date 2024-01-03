package com.andrewlalis.perfin.data;

public class DataSourceInitializationException extends Exception {
    public DataSourceInitializationException(String message) {
        super(message);
    }

    public DataSourceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
