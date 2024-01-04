package com.andrewlalis.perfin.data.pagination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SortTest {
    @Test
    public void testToSQL() {
        assertEquals("date ASC", Sort.asc("date").toSQL());
        assertEquals("id DESC", Sort.desc("id").toSQL());
    }
}
