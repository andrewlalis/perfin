package com.andrewlalis.perfin.data;

import java.sql.SQLException;

public interface SQLSupplier<T> {
    T offer() throws SQLException;
}
