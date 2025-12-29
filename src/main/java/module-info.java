module com.narrator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;

    opens com.narrator to javafx.fxml;
    exports com.narrator;
}
