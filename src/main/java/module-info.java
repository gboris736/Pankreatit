module com.pancreatitis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;


    opens com.pancreatitis to javafx.fxml;
    exports com.pancreatitis;
}