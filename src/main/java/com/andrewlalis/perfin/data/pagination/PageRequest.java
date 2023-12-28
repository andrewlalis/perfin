package com.andrewlalis.perfin.data.pagination;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record PageRequest(
        int page,
        int size,
        List<Sort> sorts
) {
    public static PageRequest of(int page, int size, Sort... sorts) {
        return new PageRequest(page, size, Arrays.asList(sorts));
    }

    public static PageRequest unpaged(Sort... sorts) {
        return new PageRequest(-1, -1, Arrays.asList(sorts));
    }

    public PageRequest next() {
        return new PageRequest(page + 1, size, sorts);
    }

    public PageRequest previous() {
        return new PageRequest(page - 1, size, sorts);
    }

    public String toSQL() {
        StringBuilder sb = new StringBuilder();
        if (!sorts.isEmpty()) {
            sb.append("ORDER BY ");
            sb.append(sorts.stream().map(Sort::toSQL).collect(Collectors.joining(", ")));
            sb.append(' ');
        }
        if (page == -1 && size == -1) {
            // Unpaged request, so return the string without any offset/limit.
            return sb.toString().strip();
        }
        long offset = (long) page * size;
        sb.append("LIMIT ").append(size).append(" OFFSET ").append(offset);
        return sb.toString();
    }
}
