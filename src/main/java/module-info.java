module com.pancreatitis.pankreat2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.pancreatitis.pankreat2 to javafx.fxml;
    exports com.pancreatitis.pankreat2;
}