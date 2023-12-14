module com.andrewlalis.perfin {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires org.xerial.sqlitejdbc;

    exports com.andrewlalis.perfin to javafx.graphics;
    opens com.andrewlalis.perfin.control to javafx.fxml;
}