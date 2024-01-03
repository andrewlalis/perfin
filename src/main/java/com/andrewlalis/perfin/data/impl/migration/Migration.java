package com.andrewlalis.perfin.data.impl.migration;

import com.andrewlalis.perfin.data.DataSource;

public interface Migration {
    void migrate(DataSource dataSource) throws Exception;
}
