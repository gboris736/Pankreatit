package com.pancreatitis.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class StartApplication {
    public Parent build() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(StartApplication.class.getResource("fxml/panelView.fxml"));
        Parent root = fxmlLoader.load();

        root.getStylesheets().add(StartApplication.class.getResource("fxml/css/style.css").toExternalForm());

        return root;

    }
}
