package com.andrewlalis.perfin.data.pagination;

public record Sort(String property, Direction direction) {
    public enum Direction {ASC, DESC}

    public static Sort asc(String property) {
        return new Sort(property, Direction.ASC);
    }

    public static Sort desc(String property) {
        return new Sort(property, Direction.DESC);
    }

    public String toSQL() {
        return property + " " + direction.name();
    }
}
