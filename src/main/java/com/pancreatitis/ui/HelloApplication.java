package com.pancreatitis.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("fxml/panelView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        scene.getStylesheets().add(HelloApplication.class.getResource("fxml/css/style.css").toExternalForm());


        stage.setTitle("Крутое Название");

        stage.centerOnScreen();

        //stage.setFullScreen(true);
        stage.setMaximized(true);

        stage.setScene(scene);
        stage.show();
    }
}
