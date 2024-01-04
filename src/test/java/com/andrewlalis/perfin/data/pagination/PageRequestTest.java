package com.andrewlalis.perfin.data.pagination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PageRequestTest {
    @Test
    public void testToSQL() {
        assertEquals("LIMIT 5 OFFSET 0", PageRequest.of(0, 5).toSQL());
        assertEquals("LIMIT 5 OFFSET 5", PageRequest.of(1, 5).toSQL());
        assertEquals("LIMIT 10 OFFSET 0", PageRequest.of(0, 10).toSQL());
        assertEquals("LIMIT 10 OFFSET 20", PageRequest.of(2, 10).toSQL());
        assertEquals("LIMIT 10 OFFSET 30", PageRequest.of(2, 10).next().toSQL());
        assertEquals("LIMIT 10 OFFSET 10", PageRequest.of(2, 10).previous().toSQL());
        assertEquals(
                "ORDER BY id DESC LIMIT 5 OFFSET 0",
                PageRequest.of(0, 5, Sort.desc("id")).toSQL()
        );
        assertEquals(
                "ORDER BY timestamp DESC, name ASC LIMIT 10 OFFSET 30",
                PageRequest.of(3, 10, Sort.desc("timestamp"), Sort.asc("name")).toSQL()
        );
        assertEquals("", PageRequest.unpaged().toSQL());
        assertEquals("ORDER BY id ASC", PageRequest.unpaged(Sort.asc("id")).toSQL());

    }
}
