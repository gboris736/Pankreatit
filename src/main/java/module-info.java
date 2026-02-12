module com.pancreatitis.pankreat2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.pancreatitis.pankreat2 to javafx.fxml;
    exports com.pancreatitis.pankreat2;
}