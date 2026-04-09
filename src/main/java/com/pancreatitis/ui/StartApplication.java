package com.pancreatitis.ui;

import com.sun.tools.javac.Main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class StartApplication {
    private FXMLLoader fxmlLoader;

    public static void main(String[] args) {
        Application.launch(Launcher.class,args);
    }


    public Parent build() throws IOException {
        fxmlLoader = new FXMLLoader(StartApplication.class.getResource("fxml/panelView.fxml"));
        Parent root = fxmlLoader.load();

        root.getStylesheets().add(StartApplication.class.getResource("fxml/css/style.css").toExternalForm());

        return root;

    }

    public MainMenuControl getControl() {
        if (fxmlLoader != null) {
            return (MainMenuControl) fxmlLoader.getController();
        }
        return null;
    }
}
