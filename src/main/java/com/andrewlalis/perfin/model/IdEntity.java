package com.andrewlalis.perfin.model;

/**
 * Base class for all entities that are identified by an id.
 */
public abstract class IdEntity {
    /**
     * The unique identifier for this entity. It distinguishes this entity from
     * all others of its type.
     */
    public final long id;

    /**
     * Constructs the entity with a given id.
     * @param id The id to use.
     */
    protected IdEntity(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IdEntity e && e.id == this.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
