package com.andrewlalis.perfin.data;

import java.io.IOException;

/**
 * Interface that defines the data source factory, a component responsible for
 * obtaining a data source, and performing some introspection around that data
 * source before one is obtained.
 */
public interface DataSourceFactory {
    DataSource getDataSource(String profileName) throws ProfileLoadException;

    enum SchemaStatus {
        UP_TO_DATE,
        NEEDS_MIGRATION,
        INCOMPATIBLE
    }
    SchemaStatus getSchemaStatus(String profileName) throws IOException;

    int getSchemaVersion(String profileName) throws IOException;
}
