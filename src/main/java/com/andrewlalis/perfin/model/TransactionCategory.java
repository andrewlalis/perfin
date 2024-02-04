package com.andrewlalis.perfin.model;

import javafx.scene.paint.Color;

public class TransactionCategory extends IdEntity {
    public static final int NAME_MAX_LENGTH = 63;

    private final Long parentId;
    private final String name;
    private final Color color;

    public TransactionCategory(long id, Long parentId, String name, Color color) {
        super(id);
        this.parentId = parentId;
        this.name = name;
        this.color = color;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return name;
    }
}
