package com.pancreatitis.ui;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.User;
import com.pancreatitis.modules.backgroundsync.BackgroundSyncModule;
import com.pancreatitis.modules.database.DatabaseModule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class StartApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // ---- Окно авторизации ----
        FXMLLoader loginLoader = new FXMLLoader(StartApplication.class.getResource("fxml/login.fxml"));
        Scene loginScene = new Scene(loginLoader.load());
        Stage loginStage = new Stage();
        loginStage.setScene(loginScene);
        loginStage.setTitle("Авторизация");
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setResizable(false);
        loginStage.showAndWait();

        LoginController loginController = loginLoader.getController();
        if (!loginController.isAuthenticated()) {
            Platform.exit();
            System.exit(0);
            return;
        }

        // ---- Сохраняем данные в User ----
        User user = User.getInstance();
        Doctor doctor = DatabaseModule.getInstance().getDoctorByLogin(loginController.getLogin());
        user.setLogin(loginController.getLogin());
        user.setKey(loginController.getSecretKey());

        user.setId(doctor.getId());
        user.setEmail(doctor.getEmail());
        user.setFio(doctor.getFio());
        user.setPhone(doctor.getPhone());
        user.setStatus(doctor.getStatus());
        user.setCreatedAt(doctor.getCreatedAt());

        // ---- Загружаем главное окно ----
        FXMLLoader fxmlLoader = new FXMLLoader(StartApplication.class.getResource("fxml/panelView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        scene.getStylesheets().add(StartApplication.class.getResource("fxml/css/style.css").toExternalForm());

        stage.setTitle("Pancreatit Manager");
        stage.centerOnScreen();
        stage.setMaximized(true);
        stage.setScene(scene);

        stage.setOnShown(e -> BackgroundSyncModule.getInstance().start());

        stage.setOnCloseRequest(e -> {
            BackgroundSyncModule.getInstance().stop();
            Platform.exit();
            System.exit(0);
        });

        stage.show();

        Label userNameLabel = (Label) scene.lookup("#userNameLabel");
        if (userNameLabel != null) {
            userNameLabel.setText(User.getInstance().getLogin());
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        BackgroundSyncModule.getInstance().stop();
    }
}