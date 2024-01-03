package com.andrewlalis.perfin.data;

public class ProfileLoadException extends Exception {
    public ProfileLoadException(String message) {
        super(message);
    }

    public ProfileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
