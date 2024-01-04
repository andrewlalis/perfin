package com.andrewlalis.perfin.data;

/**
 * Exception that's thrown when an entity of a certain type is not found
 * when it was expected that it would be.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(Class<?> type, Object id) {
        super("Entity of type " + type.getName() + " with id " + id + " was not found.");
    }
}
