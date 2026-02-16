module com.pancreatitis {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;


    opens com.pancreatitis.ui.fxml to javafx.fxml;
    exports com.pancreatitis.models;
    exports com.pancreatitis.ui;
    opens com.pancreatitis.ui to javafx.fxml;

}