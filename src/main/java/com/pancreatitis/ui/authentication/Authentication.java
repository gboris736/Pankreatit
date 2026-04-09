package com.pancreatitis.ui.authentication;

import com.pancreatitis.models.User;
import com.pancreatitis.ui.Launcher;
import com.pancreatitis.ui.StartApplication;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Authentication {
    public void setupLoginStage(Stage primaryStage) {
        // Создаем экран входа
        LoginView loginView = new LoginView();
        Scene loginScene = new Scene(loginView.getRoot(), 400, 300);

        primaryStage.setTitle("Аутентификация");
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // После успешного входа показываем главное окно
        loginView.setOnLoginSuccess(primaryStage::close);
    }
}