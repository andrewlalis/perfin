package com.andrewlalis.perfin.data;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlRunnable {
    void run() throws SQLException;
}
