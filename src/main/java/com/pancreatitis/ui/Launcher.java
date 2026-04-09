package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.ui.authentication.Authentication;
import com.pancreatitis.ui.authentication.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Launcher extends Application {
    public static Launcher instance;
    private static User user;


    @Override
    public void start(Stage primaryStage) throws IOException {
        user = User.getInstance();

        // 1. Экран авторизации
        LoginView auth = new LoginView();
        Stage authStage = new Stage();
        authStage.setTitle("Вход");
        authStage.setScene(new Scene(auth.build()));
        authStage.showAndWait(); // Ждём закрытия

        if (user == null){
            //throw new IllegalArgumentException("User is null");
            System.exit(0); // Отмена или ошибка
            return;

        }

        // 2. Главный экран
        StartApplication main = new StartApplication();
        primaryStage.setTitle("Главное меню");
        primaryStage.setScene(new Scene(main.build()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    public static synchronized Launcher getInstance() {
        if (instance == null) {
            instance = new Launcher();
        }
        return instance;
    }

    public void login(User user) {
        User.getInstance();
        User.setInstance(user);
    }

}
