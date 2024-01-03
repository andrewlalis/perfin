module com.andrewlalis.perfin {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.andrewlalis.javafx_scene_router;

    requires com.fasterxml.jackson.databind;

    requires java.sql;

    requires org.slf4j;

    exports com.andrewlalis.perfin to javafx.graphics;
    exports com.andrewlalis.perfin.view to javafx.graphics;
    exports com.andrewlalis.perfin.model to javafx.graphics;

    opens com.andrewlalis.perfin.control to javafx.fxml;
    opens com.andrewlalis.perfin.view to javafx.fxml;
    opens com.andrewlalis.perfin.view.component to javafx.fxml;
}