package com.andrewlalis.perfin.data.util;

import java.sql.SQLException;

public class UncheckedSqlException extends RuntimeException {
    public UncheckedSqlException(SQLException cause) {
        super(cause);
    }

    public UncheckedSqlException(String message, SQLException cause) {
        super(message, cause);
    }

    public UncheckedSqlException(String message) {
        super(message);
    }
}
