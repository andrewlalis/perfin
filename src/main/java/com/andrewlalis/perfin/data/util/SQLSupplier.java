package com.andrewlalis.perfin.data.util;

import java.sql.SQLException;

public interface SQLSupplier<T> {
    T offer() throws SQLException;
}
