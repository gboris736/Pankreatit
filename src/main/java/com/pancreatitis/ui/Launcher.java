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

        auth.setOnLoginSuccess(() -> {
            authStage.close();
            System.out.println("frtd");
        } );
        authStage.showAndWait(); // Ждём закрытия


        if (user == null || user.getLogin() == null){
            //throw new IllegalArgumentException("User is null");
            System.exit(0); // Отмена или ошибка
            return;

        }

        // 2. Главный экран
        StartApplication main = new StartApplication();
        primaryStage.setTitle("Главное меню");
        primaryStage.setScene(new Scene(main.build()));

        main.getControl().setUserName(user.getFullName());
        primaryStage.show();
    }


    public static synchronized Launcher getInstance() {
        if (instance == null) {
            instance = new Launcher();
        }
        return instance;
    }

    public void login(User user) {
        User user1 = user.getInstance();

        user1.setKey(user.getKey());
        user1.setLogin(user.getLogin());
        user1.setEmail(user.getEmail());
        user1.setPhone(user.getPhone());
        user1.setDoctor(user.getDoctor());
        user1.setFullName(user.getFullName());
    }

}
