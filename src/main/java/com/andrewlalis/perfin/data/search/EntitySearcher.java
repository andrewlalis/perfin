package com.andrewlalis.perfin.data.search;

import com.andrewlalis.perfin.data.pagination.Page;
import com.andrewlalis.perfin.data.pagination.PageRequest;

import java.util.List;

/**
 * An entity searcher will search for entities matching a list of filters.
 * @param <T> The entity type to search over.
 */
public interface EntitySearcher<T> {
    /**
     * Gets a page of results that match the given filters.
     * @param pageRequest The page request.
     * @param filters The filters to apply.
     * @return A page of results.
     */
    Page<T> search(PageRequest pageRequest, List<SearchFilter> filters);

    /**
     * Gets the number of results that would be returned for a given set of
     * filters.
     * @param filters The filters to apply.
     * @return The number of entities that match.
     */
    long resultCount(List<SearchFilter> filters);
}
