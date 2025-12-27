module com.narrator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.narrator to javafx.fxml;
    exports com.narrator;
}
