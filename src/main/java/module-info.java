module com.spms {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.spms.app to javafx.fxml;
    opens com.spms.model to javafx.base;
    opens com.spms.view to javafx.fxml;

    exports com.spms.app;
}
