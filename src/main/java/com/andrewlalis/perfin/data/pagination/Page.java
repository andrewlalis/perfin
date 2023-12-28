package com.andrewlalis.perfin.data.pagination;

import java.util.List;

public record Page<T>(List<T> items, PageRequest pagination) {}
